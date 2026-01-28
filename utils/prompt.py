# utils/prompt.py
import re

# BASIC_PROMPT
BASIC_PROMPT="""The following Java function contains a bug:```\n{buggy_function}```\n 
Buggy lines are marked with '// Buggy Line' as hints. The actual fix may involve changes around these lines or adding new statements if necessary. \n

Please provide a correct fix for the bug.
Your response must enclose the entire function within a ```java ... ``` block.
"""

INITIAL_PROMPT_ORIGINAL = """The following Java function contains a bug:```\n{buggy_function}```\n 
Buggy lines are marked with '// Buggy Line' as hints. The actual fix may involve changes around these lines or adding new statements if necessary. \n
The function fails on the following test(s):
{test_info}

Please provide a correct fix for the bug.
Your response must enclose the entire function within a ```java ... ``` block.
"""

ITERATIVE_PROMPT_WITH_FEEDBACK = """Your previous attempt to fix the Java function was not correct.

Here is the code you previously generated:```\n{previous_attempt_code}```\n

When validating this code, it produced the following error:```\n{error_message}```\n

Please provide a correct fix for the bug.
Your response must enclose the entire function within a ```java ... ``` block.
"""

INITIAL_PROMPT_WITH_SLICE = """The following Java function contains a bug:```\n{buggy_function}```\n 
Buggy lines are marked with '// Buggy Line' as hints. The actual fix may involve changes around these lines or adding new statements if necessary. \n
The function fails on the following test(s):
{test_info}

The following code lines, extracted through dynamic slicing, represent the minimal dependencies required to reproduce this bug. They provide additional context that may clarify the underlying cause:```\n{slice_info}```\n

Please provide a correct fix for the bug.
Your response must enclose the entire function within a ```java ... ``` block.
"""

AUGMENT_PROMPT = """
The following Java function contains a bug:```\n{buggy_function}```\n 
Buggy lines are marked with '// Buggy Line' as hints. The actual fix may involve changes around these lines or adding new statements if necessary. \n
The original code fails on the following test(s):
{test_info}

It can be fixed by this patch function:
{plausible_patch}

Please analyze all the provided information and generate a new, alternative and also correct fix of the complete Java function.
Your response must enclose the entire function within a ```java ... ```  block.
"""

def format_test_info(failing_tests_list, include_test_slice_and_deps=True):
    """
    Formats the test info from d4j_test_info.json into an English string.
    It now uses the sliced test and includes dependency information, with all
    dependencies of the same type grouped into single code blocks.
    Can optionally exclude the test snippet and dependencies for ablation studies.
    """
    info_str = ""
    valid_test_case_count = 0

    for test in failing_tests_list:
        failing_line = test.get('failing_line')
        if not failing_line or not failing_line.strip():
            continue

        valid_test_case_count += 1
        
        test_method_name = test.get('test_method_name')
        # test_snippet_code = test.get('sliced_test') or test.get('failing_function') # 这行可以移到if内部
        failure_message = test.get('failure_message')

        info_str += f"--- Test Case #{valid_test_case_count} ---\n" 
        info_str += f"Test Method: {test_method_name or 'N/A'}\n" 
        
        if include_test_slice_and_deps: 
            test_snippet_code = test.get('sliced_test') or test.get('failing_function') 
            info_str += f"Failing Test Snippet (Sliced):\n```java\n{(test_snippet_code.strip() if test_snippet_code else 'N/A')}\n```\n" #

        info_str += f"Failing Line in Test: {failing_line.strip()}\n" 
        info_str += f"Error Message: {(failure_message.strip() if failure_message else 'N/A')}\n" 

        if include_test_slice_and_deps: 
            dependencies = test.get('dependencies', {}) 
            if dependencies:
                dependent_methods = dependencies.get('methods', []) 
                dependent_variables = dependencies.get('variables', []) 

                if dependent_methods:
                    info_str += "\nThis test relies on the following external methods:\n" 
                    all_methods_code = "\n\n".join(method.strip() for method in dependent_methods) 
                    info_str += f"```java\n{all_methods_code}\n```\n" 
                
                if dependent_variables:
                    info_str += "\nThis test depends on these external fields/variables:\n" 
                    all_variables_code = "\n\n".join(var.strip() for var in dependent_variables) 
                    info_str += f"```java\n{all_variables_code}\n```\n" 
        
        info_str += "\n"

    return info_str.strip()

def format_test_info_wo_slice(failing_tests_list):
    """
    Formats the test info from d4j_test_info.json into an English string.
    It filters out and skips any test cases where 'failing_line' is empty or None.
    """
    info_str = ""
    valid_test_case_count = 0  # Counter for valid test cases only

    for test in failing_tests_list:
        # Get the failing_line first to check if it's valid
        failing_line = test.get('failing_line')

        # Skip this entire test case if failing_line is None or empty
        if not failing_line or not failing_line.strip():
            continue

        # If we are here, the test case is valid. Increment the counter.
        valid_test_case_count += 1
        
        # Get the rest of the data for the valid test case
        test_method_name = test.get('test_method_name')
        failing_function = test.get('failing_function')
        failure_message = test.get('failure_message')

        info_str += f"--- Test Case #{valid_test_case_count} ---\n"
        info_str += f"Test Method: {test_method_name or 'N/A'}\n"
        
        # Format the rest of the details for this valid test case
        info_str += f"Failing Test Snippet:\n```{(failing_function.strip() if failing_function else 'N/A')}\n```\n"
        info_str += f"Failing Line in Test: {failing_line.strip()}\n" # No need for 'else' as we already checked
        info_str += f"Error Message: {(failure_message.strip() if failure_message else 'N/A')}\n\n"
        
    return info_str.strip()

def create_feedback_message(error_info, original_test_info, include_test_source=True):
    """
    Creates a natural language feedback message based on the structured error info.
    This version now looks up the failing test's source code from the original_test_info (JSON data).
    """
    error_type = error_info['type']
    raw_message = error_info['message']
    
    # Handle compilation errors with refinement logic
    if error_type == "compilation":
        refined_errors = extract_key_errors_from_text(raw_message)
        if refined_errors:
            # If refinement is successful, use the clean error message
            return f"The Java function has the following compilation errors:\n{refined_errors.strip()}"
        else:
            # Fallback to original simple extraction if refinement yields nothing
            if "[javac]" not in raw_message:
                 return "The fixed Java function has syntax and compilation errors."
            else:
                 core_error = ":".join(raw_message.split(":")[1:])
                 return f"The Java function has the following compilation error: [javac] {core_error.strip()}"
    
    # The rest of the function for handling test failures, timeout, etc. remains unchanged.
    if error_type in ["trigger_fail", "regression_fail"]:
        new_failing_method = error_info['failing_test_method']
        
        feedback_intro = ""
        test_source_code = None

        if new_failing_method and new_failing_method != "Unknown":
            for test_case in original_test_info.get('failing_tests', []):
                if test_case.get('test_method_name') == new_failing_method:
                    test_source_code = test_case.get('failing_function')
                    break
        
        is_original_bug = any(
            test_case.get('test_method_name') == new_failing_method 
            for test_case in original_test_info.get('failing_tests', [])
        )
        
        if is_original_bug:
            feedback_intro = (f"With the repaired code you provided, the function still fails on an "
                              f"original failing test method: `{new_failing_method}`.")
        else:
            feedback_intro = (f"The function has introduced a new bug. It now fails on the following test method:\n"
                              f"`{new_failing_method.strip() if new_failing_method else 'Unknown'}`.")

        if test_source_code and include_test_source:
            feedback_intro += (f"\n\nThe source code of this failing test is:\n"
                               f"```java\n{test_source_code.strip()}\n```") 

        clean_error_message = extract_core_error_message(raw_message) 
        feedback_intro += f"\n\nThe core error log is:\n{clean_error_message.strip()}" 
        
        return feedback_intro

    if error_type == "timeout":
        return f"The test execution timed out. This might indicate an infinite loop or severe performance issue in the generated code. Error: {raw_message}"

    return f"An unknown validation error occurred. The raw error message is: {raw_message}"

def extract_core_error_message(full_log):
    """
    Extracts the core error lines from a full test log,
    stopping before the stack trace begins.
    """
    core_lines = []
    # Split the full log into lines
    lines = full_log.splitlines()
    for line in lines:
        # A stack trace line typically starts with whitespace and 'at '
        if line.strip().startswith("at "):
            break  # Stop when the stack trace starts
        core_lines.append(line)
    
    # Rejoin the collected lines, filtering out any empty lines that might have been collected
    clean_message = "\n".join(line for line in core_lines if line.strip())
    
    # Fallback to the full log if parsing fails for any reason
    return clean_message if clean_message else full_log

def extract_key_errors_from_text(log_text: str):
    """
    Extracts key, structured error messages from a verbose compilation log.
    """
    key_lines = []
    seen_messages = set()
    lines = log_text.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue

        # 1. Major version incompatibility
        m = re.search(r"major version \d+ is newer than \d+, the highest major version supported by this compiler", line)
        if m:
            msg = m.group(0)
            if msg not in seen_messages:
                key_lines.append(msg)
                seen_messages.add(msg)
            i += 1
            continue

        # 2. Bad class file
        if "bad class file" in line:
            if "bad class file" not in seen_messages:
                key_lines.append("bad class file")
                seen_messages.add("bad class file")
            i += 1
            continue

        # 3. 'cannot find symbol' error, multi-line extraction
        if "cannot find symbol" in line:
            error_block = [line]
            i += 1
            # Extract subsequent code lines and the '^' pointer
            while i < len(lines):
                next_line = lines[i]
                error_block.append(next_line)
                if re.match(r"\s*\^", next_line): # '^' line, next might be symbol/location
                    # Extract symbol and location lines if they exist
                    if i + 1 < len(lines) and "symbol:" in lines[i + 1]:
                        error_block.append(lines[i + 1])
                        i += 1
                    if i + 1 < len(lines) and "location:" in lines[i + 1]:
                        error_block.append(lines[i + 1])
                        i += 1
                    break
                i += 1
            key_lines.append("\n".join(error_block))
            i += 1
            continue

        # 4. Other keywords
        if "incompatible types" in line:
            if "incompatible types" not in seen_messages:
                key_lines.append("incompatible types")
                seen_messages.add("incompatible types")
            i += 1
            continue
            
        i += 1

    return "\n".join(key_lines)