# File: augment_patches.py

import argparse
import json
import os
import time

from utils.prompt import AUGMENT_PROMPT, format_test_info
from utils.api_request import request_engine, create_request_config
from utils.validate_d4j import validate_patch
from utils.parse_d4j import clean_parse_d4j, get_unified_diff
from iterative_repair import add_bug_comments, extract_code


PROJECT_ROOT = ""
DEFECTS4J_CMD = "defects4j/framework/bin/defects4j"
SINGLE_FUNCTION_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/single_function_repair.json")
TEST_INFO_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/d4j_test_info_with_slice.json")
LOCATION_FOLDER_PATH = os.path.join(PROJECT_ROOT, "Defects4j/location")


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
    successful_bugs = set()
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

        bug_data = all_bugs_data.get(bug_id + ".java")
        test_data = test_info_data.get(bug_id)
        if not bug_data or not test_data:
            print(f"  Warning: Missing original bug data or test info for {bug_id}. Skipping.")
            continue
        
        plausible_patch = patch_data['patch']
        results[bug_id] = results.get(bug_id, [])
        
        p_diff = {get_unified_diff(bug_data['buggy'], plausible_patch)}
        for attempt in results[bug_id]:
            p_diff.add(get_unified_diff(bug_data['buggy'], attempt['patch']))

        has_at_least_one_valid_patch = any(attempt.get('valid') for attempt in results[bug_id])

        attempts_so_far = len(results[bug_id])
        attempts_needed = args.max_attempts - attempts_so_far

        if attempts_needed <= 0:
            print(f"  Skipping {bug_id}: Already has {attempts_so_far} attempts (target {args.max_attempts}).")
            continue
        
        print(f"  {bug_id}: Needs {attempts_needed} more attempts (has {attempts_so_far}, target {args.max_attempts}).")

        while attempts_needed > 0:
            
            attempt_start_time = time.time()
            
            current_attempt_index = len(results[bug_id]) + 1  
            
            print(f"  [Attempt {current_attempt_index}/{args.max_attempts}] Generating alternative patch...")

            test_info_str = format_test_info(
                test_data['failing_tests'], 
                include_test_slice_and_deps=False
            )
            
            buggy_code_with_location = add_bug_comments(bug_data['buggy'], bug_data['location'])

            prompt = AUGMENT_PROMPT.format(
                buggy_function=buggy_code_with_location,
                test_info=test_info_str,
                plausible_patch=plausible_patch
            )

            config = create_request_config(prompt=prompt, provider=args.provider, model=args.model, temperature=args.temperature)
            model_output, token_usage = request_engine(config)
            total_llm_queries += 1
            
            new_patch = extract_code(model_output) if model_output else ""
            
            if not new_patch:
                print(f"      API request or code extraction failed for attempt {current_attempt_index}. Retrying after 5 seconds...")
                time.sleep(5) 
                continue      
            
            diff = get_unified_diff(bug_data['buggy'], new_patch)
            if diff in p_diff:
                print("      Generated a duplicate patch. Skipping validation.")

                attempt_end_time = time.time()
                attempt_duration = attempt_end_time - attempt_start_time
                
                results[bug_id].append({ 
                    "attempt": current_attempt_index, 
                    "valid": False, 
                    "message": {"type": "duplicate", "message": "Generated a duplicate patch."}, 
                    "patch": new_patch, 
                    "token_usage": token_usage,
                    "duration_seconds": attempt_duration  
                })
                
                attempts_needed -= 1
                continue 

            p_diff.add(diff)

            print("      Validating new patch...")
            is_valid, message_info = validate_patch(
                bug_id=bug_id, 
                patch=new_patch, 
                bug_data=bug_data, 
                d4j_cmd_path=DEFECTS4J_CMD, 
                loc_folder=LOCATION_FOLDER_PATH
            )

            attempt_end_time = time.time()
            attempt_duration = attempt_end_time - attempt_start_time

            results[bug_id].append({
                "attempt": current_attempt_index,
                "valid": is_valid,
                "message": message_info,  
                "patch": new_patch,             
                "token_usage": token_usage,      
                "duration_seconds": attempt_duration  
            })

            if is_valid:
                print(f"      ---> SUCCESS: A valid alternative patch was found in this attempt!")
                has_at_least_one_valid_patch = True
            else:
                print(f"      ---> FAILED: Validation error type '{message_info.get('type', 'Unknown')}'")

            attempts_needed -= 1
        
        temp_file_path = result_file_path + ".tmp"
        with open(temp_file_path, "w") as f:
            json.dump(results, f, indent=2)
        os.rename(temp_file_path, result_file_path)

        bug_end_time = time.time()
        bug_duration = bug_end_time - bug_start_time
        bug_times[bug_id] = bug_duration
        if has_at_least_one_valid_patch:
            successful_bugs.add(bug_id)
        
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
