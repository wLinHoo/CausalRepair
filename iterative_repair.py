# File Path: iterative_repair.py

import argparse
import json
import os
import re
import time
import tiktoken

# Import functions from your utility modules
from utils.prompt import (INITIAL_PROMPT_ORIGINAL,
                          INITIAL_PROMPT_WITH_SLICE,
                          ITERATIVE_PROMPT_WITH_FEEDBACK,
                          format_test_info,
                          create_feedback_message)
from utils.api_request import request_engine, create_request_config
from utils.validate_d4j import validate_patch
from utils.parse_d4j import clean_parse_d4j, get_unified_diff

# ===================================================================================
# Core Path Configurations
# ===================================================================================
PROJECT_ROOT = "../CausalRepair"
DEFECTS4J_CMD = "../defects4j/framework/bin/defects4j"
SINGLE_FUNCTION_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/single_function_repair.json")
TEST_INFO_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/d4j_test_info_sf.json")
SLICE_INFO_JSON_PATH = os.path.join(PROJECT_ROOT, "Defects4j/d4j_slice_info.json")
LOCATION_FOLDER_PATH = os.path.join(PROJECT_ROOT, "Defects4j/location")
# ===================================================================================

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

def extract_code(text):
    # ... (This function remains unchanged)
    code_pattern = re.compile(r'```(?:java)?\s*\n(.*?)\n```', re.DOTALL)
    match = code_pattern.search(text)
    if match:
        return match.group(1).strip()
    return text.strip()

def add_bug_comments(code_string, buggy_line_numbers):
    # ... (This function remains unchanged)
    lines = code_string.split("\n")
    for line_num in buggy_line_numbers:
        if 1 <= line_num <= len(lines):
            lines[line_num - 1] += " // Buggy Line"
    return "\n".join(lines)

def shrink_prompt_if_needed(prompt_template, buggy_function, slice_info, test_info, threshold, tokenizer):
    """
    Shrinks the prompt if its token count exceeds the threshold.
    Includes a 5-minute timeout to prevent getting stuck.
    Returns: A tuple (prompt, timed_out), where timed_out is True if the process timed out.
    """
    prompt = prompt_template.format(buggy_function=buggy_function, slice_info=slice_info, test_info=test_info)
    current_tokens = len(tokenizer.encode(prompt))

    if current_tokens <= threshold:
        return prompt, False  # Prompt is within limits, no timeout.

    print(f"      Warning: Initial prompt token count {current_tokens} exceeds threshold {threshold}. Shrinking...")
    
    start_time = time.time()
    timeout_seconds = 600  # 5 minutes

    slice_lines = slice_info.splitlines()
    test_lines = test_info.splitlines()

    while current_tokens > threshold:
        if time.time() - start_time > timeout_seconds:
            print(f"      Timeout: Shrinking process exceeded {timeout_seconds} seconds. Aborting shrink.")
            return "", True  # Return an empty prompt and True for timeout.

        if slice_lines:
            slice_lines.pop()
            slice_info = "\n".join(slice_lines)
        elif test_lines:
            test_lines.pop()
            test_info = "\n".join(test_lines)
        else:
            print("      Warning: Slice and test info are empty, but token count is still over the limit.")
            break
        
        prompt = prompt_template.format(buggy_function=buggy_function, slice_info=slice_info, test_info=test_info)
        current_tokens = len(tokenizer.encode(prompt))

    print(f"      Shrinking complete. Final prompt token count: {current_tokens}")
    return prompt, False # Shrinking finished successfully, no timeout.

def shrink_feedback_prompt_if_needed(prompt_template, previous_attempt_code, error_message, threshold, tokenizer):
    # ... (This function remains unchanged)
    prompt = prompt_template.format(previous_attempt_code=previous_attempt_code, error_message=error_message)
    current_tokens = len(tokenizer.encode(prompt))
    if current_tokens <= threshold:
        return prompt
    print(f"      Warning: Feedback prompt token count {current_tokens} exceeds threshold {threshold}. Shrinking...")
    error_lines = error_message.splitlines()
    code_lines = previous_attempt_code.splitlines()
    while current_tokens > threshold:
        if error_lines:
            error_lines.pop()
            error_message = "\n".join(error_lines)
        elif code_lines:
            code_lines.pop()
            previous_attempt_code = "\n".join(code_lines)
        else:
            print("      Warning: Error message and previous code are empty, but token count is still over the limit.")
            break
        prompt = prompt_template.format(previous_attempt_code=previous_attempt_code, error_message=error_message)
        current_tokens = len(tokenizer.encode(prompt))
    print(f"      Shrinking complete. Final feedback prompt token count: {current_tokens}")
    return prompt

def log_statistics(output_folder, current_total_duration, total_queries, bug_repair_times, successfully_repaired_bugs, successful_bug_repair_times):
    """
    Calculates and logs all statistics to a text file. Can be called incrementally.
    """
    log_file_path = os.path.join(output_folder, "repair_statistics.txt")

    total_bugs_processed = len(bug_repair_times)
    num_successful_bugs = len(successfully_repaired_bugs)

    # Calculate averages, handling division by zero
    avg_time_per_bug = current_total_duration / total_bugs_processed if total_bugs_processed > 0 else 0
    avg_time_per_successful_bug = sum(successful_bug_repair_times) / num_successful_bugs if num_successful_bugs > 0 else 0

    with open(log_file_path, "w") as f:
        f.write("========== Repair Process Statistics ==========\n\n")
        f.write(f"Total LLM Queries: {total_queries}\n")
        f.write(f"Total Bugs Processed: {total_bugs_processed}\n")
        f.write(f"Successfully Repaired Bugs: {num_successful_bugs}\n")
        f.write("\n")
        f.write("---------- Time Statistics ----------\n")
        f.write(f"Total Repair Time (So Far): {current_total_duration:.2f} seconds ({current_total_duration/60:.2f} minutes)\n")
        f.write(f"Average Time per Bug (Overall): {avg_time_per_bug:.2f} seconds\n")
        f.write(f"Average Time per SUCCESSFULLY Repaired Bug: {avg_time_per_successful_bug:.2f} seconds\n")
        f.write("\n")
        f.write("---------- Individual Bug Details (Time Taken) ----------\n")
        # Sort bugs by time taken, descending
        sorted_bug_times = sorted(bug_repair_times.items(), key=lambda item: item[1], reverse=True)
        for bug_id, duration in sorted_bug_times:
            status = "SUCCESS" if bug_id in successfully_repaired_bugs else "FAIL"
            f.write(f"Bug ID: {bug_id:<15} | Status: {status:<7} | Time: {duration:.2f} seconds\n")
        f.write("\n")
        f.write("---------- Successfully Repaired Bug List ----------\n")
        if successfully_repaired_bugs:
            for bug_id in sorted(successfully_repaired_bugs):
                f.write(f"- {bug_id}\n")
        else:
            f.write("None\n")

def repair_iterative(args, bugs_to_repair, test_info_data, slice_info_data, tokenizer, process_start_time):
    """
    The core program repair logic. Now handles interruptions safely, logs stats incrementally,
    and retries API failures without consuming an attempt count.
    """
    total_llm_queries = 0
    successfully_repaired_bugs = []
    bug_repair_times = {}
    successful_bug_repair_times = []

    output_folder = args.folder
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    result_file_path = os.path.join(output_folder, "repair_result_iterative.json")
    try:
        with open(result_file_path, "r") as f:
            result = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError): # Handle corrupted JSON on start
        result = {}

    repaired_bug_ids_set = set(result.keys())
    
    bugs_to_process_list = []
    all_bug_items_list = list(bugs_to_repair.items()) 
    all_bug_ids_list = [fname.replace(".java", "") for fname in bugs_to_repair.keys()] 

    for file_name, bug_data in all_bug_items_list:
        bug_id = file_name.replace(".java", "")
        if bug_id not in repaired_bug_ids_set:
            bugs_to_process_list.append((file_name, bug_data)) 

    total_bugs_remaining = len(bugs_to_process_list)
    total_bugs_all = len(all_bug_items_list)
    total_repaired_count = len(repaired_bug_ids_set)
    
    if not bugs_to_process_list:
        print(f"All {total_bugs_all} bugs already exist in the result file. Task completed.")
    else:
        first_missing_bug_id = bugs_to_process_list[0][0].replace(".java", "")
        print(f"Out of {total_bugs_all} total bugs, {total_repaired_count} bugs are already present in the results.")
        print(f"Starting from the first missing bug ({first_missing_bug_id}), a total of {total_bugs_remaining} remaining bugs need to be repaired.")

    
    bug_count = 0
    total_bugs = len(bugs_to_process_list)
    for file_name, bug_data in bugs_to_process_list:
        bug_start_time = time.time()
        is_bug_repaired_successfully = False

        bug_count += 1
        bug_id = file_name.replace(".java", "")
        # print(f"[{bug_count}/{total_bugs}] Starting repair for: {bug_id} (Overall index: {start_index + bug_count - 1})")
        original_index = all_bug_ids_list.index(bug_id)
        print(f"[{bug_count}/{total_bugs}] Starting repair for: {bug_id} (Overall index: {original_index})")

        if bug_id not in test_info_data:
            print(f"  Warning: Test info for {bug_id} not found. Skipping.")
            continue

        result[bug_id] = result.get(bug_id, [])
        
        for round_num in range(1, args.max_rounds + 1):
            print(f"  [Round {round_num}/{args.max_rounds}] Starting...")
            
            is_round_successful = False
            p_diff = {} 
            current_code_to_fix = bug_data['buggy']
            last_error_info = {}
            
            attempt_num = 1
            while attempt_num <= args.max_attempts_per_round:
                attempt_start_time = time.time()
                print(f"    [Attempt {attempt_num}/{args.max_attempts_per_round}] Generating patch...")

                prompt = ""
                # Use last_error_info to decide if this is a feedback-based attempt
                if attempt_num == 1 and not last_error_info:
                    buggy_code_with_location = add_bug_comments(current_code_to_fix, bug_data['location'])
                    failing_tests_list_full = test_info_data[bug_id]['failing_tests']
                    failing_tests_list = []
                    if args.test_case_mode == "one":
                        print("      (Using only the first failing test case)")
                        failing_tests_list = failing_tests_list_full[:1]
                    else:
                        print("      (Using all failing test cases)")
                        failing_tests_list = failing_tests_list_full
                    # test_info_str = format_test_info(failing_tests_list)
                    test_info_str = format_test_info(failing_tests_list, include_test_slice_and_deps=(not args.ablation_no_test_slice))
                    
                    # Determine which prompt template to use
                    prompt_template = INITIAL_PROMPT_WITH_SLICE if args.use_slice else INITIAL_PROMPT_ORIGINAL
                    
                    # Prepare slice_str if use_slice is enabled
                    slice_str = ""
                    if args.use_slice:
                        bug_slice_data = slice_info_data.get(bug_id)
                        if isinstance(bug_slice_data, dict):
                            slice_info_parts = []
                            for test_case in failing_tests_list:
                                test_path = test_case.get('test_file_path')
                                test_name = test_case.get('test_method_name')
                                if not test_path or not test_name:
                                    continue
                                full_test_name = f"{test_path}::{test_name}"
                                slices_for_test = bug_slice_data.get(full_test_name)
                                if slices_for_test:
                                    current_test_slice_parts = []
                                    for slice_item in slices_for_test:
                                        file_path = slice_item.get("file_path", "N/A")
                                        comment = slice_item.get("comment", "")
                                        sliced_code = slice_item.get("sliced_code_relative", "")
                                        prefix_to_remove = ""
                                        file_path = file_path.replace(prefix_to_remove, "")
                                        slice_str_part = f"File: {file_path}\n"
                                        code_content_parts = []
                                        if comment and comment != "No JavaDoc available.":
                                            code_content_parts.append(f"/**\n{comment.strip()}\n */")
                                        if sliced_code:
                                            code_content_parts.append(sliced_code.strip())
                                        full_code_content = "\n".join(code_content_parts)
                                        slice_str_part += f"```java\n{full_code_content}\n```"
                                        current_test_slice_parts.append(slice_str_part)
                                    if current_test_slice_parts:
                                        slice_info_parts.append(
                                            f"Dynamic slice information from test '{test_name}':\n\n" + 
                                            "\n\n".join(current_test_slice_parts)
                                        )
                            slice_str = "\n\n".join(slice_info_parts)
                        elif isinstance(bug_slice_data, str) and bug_slice_data:
                            print("      (Warning: Found old string format for slice data. Using it directly.)")
                            slice_str = bug_slice_data
                        if not slice_str:
                             slice_str = "// Program slice data is not available for this bug."

                    # Call the shrinking function
                    prompt, timed_out = shrink_prompt_if_needed(
                        prompt_template, 
                        buggy_code_with_location, 
                        slice_str, 
                        test_info_str, 
                        args.token_threshold, 
                        tokenizer
                    )

                    # Handle the timeout according to the new requirement
                    if timed_out:
                        print("      Applying fallback: Removing DYNAMIC SLICE info due to timeout, but keeping test info.")
                        # Define a placeholder for the removed slice information
                        slice_fallback = "// [INFO] Program slice data was removed due to a timeout during prompt shrinking."
                        
                        # Re-generate the prompt, REMOVING ONLY THE SLICE, BUT KEEPING THE ORIGINAL TEST INFO
                        prompt = prompt_template.format(
                            buggy_function=buggy_code_with_location,
                            slice_info=slice_fallback if args.use_slice else "", # Use fallback for slice
                            test_info=test_info_str  # <-- KEY CHANGE: Use the original full test info
                        )
                        print("      Fallback prompt generated with test info intact.")

                    # print(prompt)
                else: 
                    original_test_info = test_info_data[bug_id]
                    # feedback_message = create_feedback_message(last_error_info, original_test_info)
                    feedback_message = create_feedback_message(
                        last_error_info, 
                        original_test_info,
                        include_test_source=(not args.ablation_no_test_slice)
                    )
                    prompt = shrink_feedback_prompt_if_needed(prompt_template=ITERATIVE_PROMPT_WITH_FEEDBACK, previous_attempt_code=current_code_to_fix, error_message=feedback_message, threshold=args.token_threshold, tokenizer=tokenizer)
                    # print(prompt)

                config = create_request_config(prompt=prompt, provider=args.provider, model=args.model)
                # model_output = request_engine(config)
                model_output, token_usage = request_engine(config)
                total_llm_queries += 1
                
                patch_code = extract_code(model_output) if model_output else ""

                # If API request fails or code extraction fails, retry this attempt
                if not model_output or not patch_code:
                    print(f"      API request or code extraction failed for attempt {attempt_num}. Retrying after 5 seconds...")
                    time.sleep(5) # Wait 5 seconds before retrying
                    continue      # This skips the rest of the loop and retries the same attempt number

                diff = get_unified_diff(bug_data['buggy'], patch_code)
                if diff in p_diff:
                    print("      Generated a duplicate patch. Skipping validation.")
                    attempt_num += 1 # A duplicate patch still counts as a valid attempt
                    continue
                p_diff[diff] = True

                print("      Validating patch...")
                is_valid, message_info = validate_patch(bug_id=bug_id, patch=patch_code, bug_data=bug_data, d4j_cmd_path=DEFECTS4J_CMD, loc_folder=LOCATION_FOLDER_PATH)
                
                attempt_end_time = time.time()
                attempt_duration_seconds = attempt_end_time - attempt_start_time

                result[bug_id].append({
                    "round": round_num, 
                    "attempt": attempt_num, 
                    "valid": is_valid,
                    "message": message_info, 
                    "patch": patch_code,
                    "token_usage": token_usage,
                    "attempt_duration_seconds": attempt_duration_seconds 
                })               
                # Use atomic write to save the results safely
                temp_file_path = result_file_path + ".tmp"
                with open(temp_file_path, "w") as f:
                    json.dump(result, f, indent=2)
                os.rename(temp_file_path, result_file_path)
                
                if not is_valid and message_info.get('type') == 'checkout_fail':
                    print(f"      Environment Error: Defects4J checkout failed. Retrying the entire attempt {attempt_num} after a delay...")
                    time.sleep(10)
                    continue 
                    
                if is_valid:
                    print(f"  SUCCESS: A valid patch for {bug_id} was found!")
                    is_round_successful = True
                    is_bug_repaired_successfully = True
                    break # Break out of the while loop for attempts
                else:
                    print(f"      FAILED: Validation error type '{message_info['type']}'")
                    current_code_to_fix = patch_code
                    last_error_info = message_info
                
                # Increment attempt number only after a successful API call and validation cycle
                attempt_num += 1

            if is_round_successful:
                break
        
        bug_end_time = time.time()
        bug_duration = bug_end_time - bug_start_time
        bug_repair_times[bug_id] = bug_duration
        if is_bug_repaired_successfully:
            successfully_repaired_bugs.append(bug_id)
            successful_bug_repair_times.append(bug_duration)

        # Update statistics file after each bug is processed
        duration_so_far = time.time() - process_start_time
        log_statistics(
            output_folder, duration_so_far, total_llm_queries, 
            bug_repair_times, successfully_repaired_bugs, successful_bug_repair_times
        )

def main():
    parser = argparse.ArgumentParser(description="Iterative Program Repair using LLMs")
    
    parser.add_argument(
        "--provider", 
        type=str, 
        default="zhipu", 
        choices=["siliconflow", "openai", "zhipu"], 
        help="The API provider to use."
    )
    parser.add_argument(
        "--model", 
        type=str, 
        default="deepseek-v3-250324", 
        help="The specific model name to use for the chosen provider."
    )
    
    parser.add_argument("--token_threshold", type=int, default=100000, help="The maximum token threshold for the initial repair prompt.")
    parser.add_argument("--folder", type=str, required=True, help="Folder to store results.")
    parser.add_argument("--max_rounds", type=int, default=5, help="Max number of new repair rounds.")
    parser.add_argument("--max_attempts_per_round", type=int, default=3, help="Max iterative attempts per round.")
    parser.add_argument("--d4j_version", type=str, default="all", choices=["1.2", "2.0", "all"], help="Defects4J version.")
    parser.add_argument("--use_slice", action="store_true", help="Include program slice info in the prompt.")
    parser.add_argument("--test_case_mode", type=str, default="all", choices=["all", "one"], help="Controls how many failing test cases to provide in the prompt: 'all' or just the 'one'.")
    parser.add_argument(
        "--ablation-no-test-slice", 
        action="store_true", 
        help="Ablation study flag: If set, removes the test code snippet and its dependencies from the prompt."
    )
    args = parser.parse_args()

    print("0. Initializing tiktoken tokenizer (cl100k_base)...")
    tokenizer = tiktoken.get_encoding("cl100k_base")
    print("1. Loading and parsing Defects4J bug data...")
    all_bugs_data = clean_parse_d4j(SINGLE_FUNCTION_JSON_PATH)
    print(f"   Loaded data for {len(all_bugs_data)} bug functions.")
    print("2. Loading detailed test failure information...")
    try:
        with open(TEST_INFO_JSON_PATH, "r") as f:
            test_info_data = json.load(f)
        print(f"   Loaded test info for {len(test_info_data)} bugs.")
    except FileNotFoundError:
        print(f"   ERROR: Test info file not found at: {TEST_INFO_JSON_PATH}")
        return
    print("3. Loading dynamic program slice information...")
    try:
        with open(SLICE_INFO_JSON_PATH, "r") as f:
            slice_info_data = json.load(f)
        print(f"   Loaded slice info for {len(slice_info_data)} bugs.")
    except FileNotFoundError:
        print(f"   WARNING: Slice info file not found at: {SLICE_INFO_JSON_PATH}")
        slice_info_data = {}
    bugs_to_run = {}
    if args.d4j_version == "all":
        bugs_to_run = all_bugs_data
    else:
        d4j1_2_ids = build_d4j1_2()
        if args.d4j_version == "1.2":
            bugs_to_run = {key: value for key, value in all_bugs_data.items() if key.split('.')[0] in d4j1_2_ids}
        elif args.d4j_version == "2.0":
            bugs_to_run = {key: value for key, value in all_bugs_data.items() if key.split('.')[0] not in d4j1_2_ids}
    if not bugs_to_run:
        print("   ERROR: No bugs were found to run.")
        return

    print(f"4. Starting repair process (Provider: {args.provider}, Model: {args.model}, Target: {len(bugs_to_run)} bugs)...")
    
    process_start_time = time.time()
    
    repair_iterative(
        args, bugs_to_run, test_info_data, slice_info_data, tokenizer, process_start_time
    )
    
    process_end_time = time.time()
    total_duration = process_end_time - process_start_time
    
    print(f"\nStatistics have been saved to {os.path.join(args.folder, 'repair_statistics.txt')}")
    print(f"All repair tasks completed in {total_duration:.2f} seconds.")


if __name__ == "__main__":
    main()
