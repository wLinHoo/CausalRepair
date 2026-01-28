# File: augment_patches.py (Upgraded Version 2.0)

import argparse
import json
import os
import time

# 从您现有的工具模块中导入所需函数
from utils.prompt import AUGMENT_PROMPT, format_test_info
from utils.api_request import request_engine, create_request_config
from utils.validate_d4j import validate_patch
from utils.parse_d4j import clean_parse_d4j, get_unified_diff
# 从 iterative_repair.py 导入 add_bug_comments 和 extract_code 函数
from iterative_repair import add_bug_comments, extract_code


# 核心路径配置 (与 iterative_repair.py 保持一致)
PROJECT_ROOT = "/root/autodl-tmp/APR/slicer4repair-patch-aug"
DEFECTS4J_CMD = "/root/autodl-tmp/APR/defects4j/framework/bin/defects4j"
SINGLE_FUNCTION_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/single_function_repair.json")
TEST_INFO_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/d4j_test_info_with_slice.json")
LOCATION_FOLDER_PATH = os.path.join(PROJECT_ROOT, "Defects4j/location")


# ★★★ 新增点 1: 增加 Defects4J 1.2 版本 bug 列表的辅助函数 ★★★
def build_d4j1_2():
    """
    Returns a list of bug IDs for Defects4J v1.2.
    """
    bugs = []
    # Chart
    for i in range(1, 27):
        bugs.append('Chart-{}'.format(i))
    # Closure
    for i in range(1, 134):
        if i != 63 and i != 93:
            bugs.append('Closure-{}'.format(i))
    # Lang
    for i in range(1, 66):
        if i != 2:
            bugs.append('Lang-{}'.format(i))
    # Math
    for i in range(1, 107):
        bugs.append('Math-{}'.format(i))
    # Mockito
    for i in range(1, 39):
        bugs.append('Mockito-{}'.format(i))
    # Time
    for i in range(1, 28):
        if i != 21:
            bugs.append('Time-{}'.format(i))
    return bugs


def log_augmentation_stats(output_folder, total_duration, total_queries, bug_times, successful_bugs):
    """
    记录补丁增强过程的统计数据。
    """
    log_file_path = os.path.join(output_folder, "augmentation_statistics.txt")
    
    num_total_bugs = len(bug_times)
    num_successful_bugs = len(successful_bugs)
    successful_bug_times = [bug_times[bug_id] for bug_id in successful_bugs]
    
    avg_time_per_bug = total_duration / num_total_bugs if num_total_bugs > 0 else 0
    avg_time_per_successful_bug = sum(successful_bug_times) / num_successful_bugs if num_successful_bugs > 0 else 0

    with open(log_file_path, "w") as f:
        f.write("========== Patch Augmentation Statistics ==========\n\n")
        f.write(f"Total LLM Queries: {total_queries}\n")
        f.write(f"Total Bugs Processed: {num_total_bugs}\n")
        f.write(f"Successfully Repaired Bugs (at least one valid patch): {num_successful_bugs}\n")
        f.write("\n")
        f.write("---------- Time Statistics ----------\n")
        f.write(f"Total Augmentation Time: {total_duration:.2f} seconds ({total_duration/60:.2f} minutes)\n")
        f.write(f"Average Time per Bug (Overall): {avg_time_per_bug:.2f} seconds\n")
        f.write(f"Average Time per SUCCESSFULLY Repaired Bug: {avg_time_per_successful_bug:.2f} seconds\n")
        f.write("\n")
        f.write("---------- Individual Bug Details (Time Taken) ----------\n")
        sorted_bug_times = sorted(bug_times.items(), key=lambda item: item[1], reverse=True)
        for bug_id, duration in sorted_bug_times:
            status = "SUCCESS" if bug_id in successful_bugs else "FAIL"
            f.write(f"Bug ID: {bug_id:<15} | Status: {status:<7} | Time: {duration:.2f} seconds\n")
        f.write("\n")
        f.write("---------- Successfully Repaired Bug List ----------\n")
        if successful_bugs:
            for bug_id in sorted(successful_bugs):
                f.write(f"- {bug_id}\n")
        else:
            f.write("None\n")

def run_augmentation(args, all_bugs_data, test_info_data, bugs_to_process):
    """
    补丁增强的核心逻辑。
    """
    process_start_time = time.time()
    total_llm_queries = 0
    successful_bugs = set() # 改为 set 以方便添加
    bug_times = {}

    output_folder = args.output_folder
    os.makedirs(output_folder, exist_ok=True)
    result_file_path = os.path.join(output_folder, "augmentation_results.json")
    
    try:
        with open(result_file_path, "r") as f:
            results = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        results = {}

    bug_count = 0
    total_bugs_to_process_count = len(bugs_to_process)

    for bug_id, patch_data in bugs_to_process.items():
        bug_start_time = time.time()
        bug_count += 1
        print(f"\n[{bug_count}/{total_bugs_to_process_count}] Starting augmentation for: {bug_id}")

        if bug_id in results and len(results[bug_id]) >= args.max_attempts:
             print(f"  Skipping {bug_id}: Already has {len(results[bug_id])} attempts in previous results.")
             continue

        # 获取所需的数据
        bug_data = all_bugs_data.get(bug_id + ".java")
        test_data = test_info_data.get(bug_id)
        if not bug_data or not test_data:
            print(f"  Warning: Missing original bug data or test info for {bug_id}. Skipping.")
            continue
        
        plausible_patch = patch_data['patch']
        # 初始化或加载已有结果
        results[bug_id] = results.get(bug_id, [])
        
        # 初始化 p_diff，包含 plausible patch 和所有历史尝试
        p_diff = {get_unified_diff(bug_data['buggy'], plausible_patch)}
        for attempt in results[bug_id]:
            p_diff.add(get_unified_diff(bug_data['buggy'], attempt['patch']))

        has_at_least_one_valid_patch = any(attempt.get('valid') for attempt in results[bug_id])

        # ★★★ 修改点 2: 核心逻辑修改，确保执行固定次数的尝试 (已更新为 while 循环) ★★★
        # 计算还需要多少次 *成功* 的尝试
        attempts_so_far = len(results[bug_id])
        attempts_needed = args.max_attempts - attempts_so_far

        if attempts_needed <= 0:
            print(f"  Skipping {bug_id}: Already has {attempts_so_far} attempts (target {args.max_attempts}).")
            continue
        
        print(f"  {bug_id}: Needs {attempts_needed} more attempts (has {attempts_so_far}, target {args.max_attempts}).")

        # 使用 while 循环来确保我们 *成功* 完成 'attempts_needed' 次API调用
        # 借鉴 iterative_repair.py 中的重试逻辑
        # while attempts_needed > 0:
            
        #     # current_attempt_index 应该是基于 *已记录* 的结果
        #     current_attempt_index = len(results[bug_id]) + 1
        while attempts_needed > 0:
            
            # <<< 新增：记录单次尝试的开始时间
            attempt_start_time = time.time()
            
            # current_attempt_index 应该是基于 *已记录* 的结果
            current_attempt_index = len(results[bug_id]) + 1  
            
            print(f"  [Attempt {current_attempt_index}/{args.max_attempts}] Generating alternative patch...")

            # 格式化测试信息
            test_info_str = format_test_info(
                test_data['failing_tests'], 
                include_test_slice_and_deps=False
            )
            
            # 为缺陷代码添加位置信息
            buggy_code_with_location = add_bug_comments(bug_data['buggy'], bug_data['location'])

            # 构建Prompt
            prompt = AUGMENT_PROMPT.format(
                buggy_function=buggy_code_with_location,
                test_info=test_info_str,
                plausible_patch=plausible_patch
            )
            # print(prompt)

            # 请求LLM
            config = create_request_config(prompt=prompt, provider=args.provider, model=args.model, temperature=args.temperature)
            model_output, token_usage = request_engine(config)
            total_llm_queries += 1
            
            # 提取代码
            new_patch = extract_code(model_output) if model_output else ""
            
            # --- ★★★ 这就是您要的新逻辑 ★★★ ---
            # 如果 API 请求失败或代码提取失败，重试
            if not new_patch:
                print(f"      API request or code extraction failed for attempt {current_attempt_index}. Retrying after 5 seconds...")
                time.sleep(5) # 等待 5 秒
                continue      # 'continue' 将会重新开始 while 循环，而 'attempts_needed' 计数器不会减少
                              # 这将重试同一次 attempt
            
            # --- 检查重复 (逻辑不变) ---
            diff = get_unified_diff(bug_data['buggy'], new_patch)
            if diff in p_diff:
                print("      Generated a duplicate patch. Skipping validation.")
                
                # <<< 新增：计算耗时
                attempt_end_time = time.time()
                attempt_duration = attempt_end_time - attempt_start_time
                
                results[bug_id].append({ 
                    "attempt": current_attempt_index, 
                    "valid": False, 
                    "message": {"type": "duplicate", "message": "Generated a duplicate patch."}, 
                    "patch": new_patch, 
                    "token_usage": token_usage,
                    "duration_seconds": attempt_duration  # <<< 新增字段
                })
                
                # 即使是重复的，也算作一次 *已完成* 的尝试，所以我们要减少计数器
                attempts_needed -= 1
                continue # 'continue' 将会进入下一次 'while' 循环

            p_diff.add(diff)

            # --- 验证新补丁 (逻辑不变) ---
            print("      Validating new patch...")
            is_valid, message_info = validate_patch(
                bug_id=bug_id, 
                patch=new_patch, 
                bug_data=bug_data, 
                d4j_cmd_path=DEFECTS4J_CMD, 
                loc_folder=LOCATION_FOLDER_PATH
            )

            # <<< 新增：计算耗时
            attempt_end_time = time.time()
            attempt_duration = attempt_end_time - attempt_start_time

            # --- 记录结果 (逻辑不变) ---
            results[bug_id].append({
                "attempt": current_attempt_index,
                "valid": is_valid,
                "message": message_info,         # (原代码中已有)
                "patch": new_patch,              # (原代码中已有)
                "token_usage": token_usage,      # (原代码中已有)
                "duration_seconds": attempt_duration  # <<< 新增字段
            })

            if is_valid:
                print(f"      ---> SUCCESS: A valid alternative patch was found in this attempt!")
                has_at_least_one_valid_patch = True
            else:
                print(f"      ---> FAILED: Validation error type '{message_info.get('type', 'Unknown')}'")

            # --- ★★★ 标记一次尝试已完成 ★★★ ---
            # 只有当一次完整的尝试（非API失败）完成后，才减少计数器
            attempts_needed -= 1
        
        # 循环结束后，原子性地写入一次结果文件
        temp_file_path = result_file_path + ".tmp"
        with open(temp_file_path, "w") as f:
            json.dump(results, f, indent=2)
        os.rename(temp_file_path, result_file_path)

        # 记录本次bug处理的时间和状态
        bug_end_time = time.time()
        bug_duration = bug_end_time - bug_start_time
        bug_times[bug_id] = bug_duration
        if has_at_least_one_valid_patch:
            successful_bugs.add(bug_id)
        
        # 实时更新统计数据
        duration_so_far = time.time() - process_start_time
        log_augmentation_stats(output_folder, duration_so_far, total_llm_queries, bug_times, list(successful_bugs))


def main():
    parser = argparse.ArgumentParser(description="Augment plausible patches to find correct fixes.")
    parser.add_argument(
        "--input_plausible", 
        type=str, 
        required=True, 
        help="Path to the `plausible_patches.json` file."
    )
    parser.add_argument(
        "--output_folder", 
        type=str, 
        required=True, 
        help="Folder to store augmentation results and statistics."
    )
    parser.add_argument(
        "--provider", 
        type=str, 
        default="siliconflow", 
        choices=["siliconflow", "openai", "zhipu"], 
        help="The API provider to use."
    )
    parser.add_argument(
        "--model", 
        type=str, 
        default="deepseek-ai/DeepSeek-V3", 
        help="The specific model name to use."
    )
    parser.add_argument(
        "--max_attempts", 
        type=int, 
        default=40, 
        help="The fixed number of new alternative patches to generate for each plausible patch."
    )
    # ★★★ 新增点 3: 增加 d4j_version 命令行参数 ★★★
    parser.add_argument(
        "--d4j_version", 
        type=str, 
        default="all", 
        choices=["1.2", "2.0", "all"], 
        help="Filter bugs by Defects4J version."
    )
    parser.add_argument(
        "--temperature",
        type=float,
        default=1.0,
        help="The temperature for LLM sampling."
    )
    args = parser.parse_args()

    print("1. Loading original bug data...")
    all_bugs_data = clean_parse_d4j(SINGLE_FUNCTION_JSON_PATH)

    print("2. Loading detailed test failure information...")
    with open(TEST_INFO_JSON_PATH, "r") as f:
        test_info_data = json.load(f)

    print(f"3. Loading plausible patches from: {args.input_plausible}")
    with open(args.input_plausible, "r") as f:
        plausible_patches = json.load(f)

    # ★★★ 新增点 4: 根据 d4j_version 参数筛选要运行的 bugs ★★★
    bugs_to_run = {}
    print(f"4. Filtering bugs for Defects4J version: {args.d4j_version}")
    if args.d4j_version == "all":
        bugs_to_run = plausible_patches
    else:
        d4j1_2_ids = build_d4j1_2()
        if args.d4j_version == "1.2":
            bugs_to_run = {
                bug_id: data for bug_id, data in plausible_patches.items() if bug_id in d4j1_2_ids
            }
        elif args.d4j_version == "2.0":
            bugs_to_run = {
                bug_id: data for bug_id, data in plausible_patches.items() if bug_id not in d4j1_2_ids
            }
    
    if not bugs_to_run:
        print("   ERROR: No bugs found to run after filtering. Please check your plausible patches file and version selection.")
        return

    print(f"5. Starting augmentation process for {len(bugs_to_run)} bugs...")
    run_augmentation(args, all_bugs_data, test_info_data, bugs_to_run)
    
    print("\nAll augmentation tasks completed.")

if __name__ == "__main__":
    main()