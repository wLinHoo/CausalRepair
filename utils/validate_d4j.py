# utils/validate_d4j.py
import json
import subprocess
import os
import re

def parse_d4j_test_output(output):
    """
    Parses the output of a Defects4J test run to extract a clean method name.
    Handles formats like '...ClassName::methodName' and '...methodName(ClassName)'.
    """
    failing_test_method = "Unknown"
    
    # Priority 1: Match format like ' - org.jfree.data.time.junit.TimeSeriesTests::testCreateCopy2'
    match = re.search(r'-\s+[\w\.$]+::(\w+)', output)
    if match:
        failing_test_method = match.group(1)
        return failing_test_method, "Unknown"

    # Priority 2: Match format like '... testSerialization(org.jfree.chart.util.junit.AbstractObjectListTests)'
    match = re.search(r'-\s(.*?)\(', output)
    if match:
        full_method_name = match.group(1).strip()
        if '.' in full_method_name:
            failing_test_method = full_method_name.split('.')[-1]
        else:
            failing_test_method = full_method_name
        return failing_test_method, "Unknown"

    # Priority 3: Fallback for other simple formats
    match = re.search(r'-\s(.*?)$', output, re.MULTILINE)
    if match:
        full_method_name = match.group(1).strip()
        if '.' in full_method_name:
            failing_test_method = full_method_name.split('.')[-1]
        else:
            failing_test_method = full_method_name
        return failing_test_method, "Unknown"
        
    return failing_test_method, "Unknown"

def run_d4j_test(d4j_cmd_path, work_dir, test_methods):
    """
    Runs Defects4J tests and returns a structured error dictionary on failure.
    This version no longer extracts source code.
    """
    error_info = {"type": "pass", "message": "All tests passed", "failing_test_method": None}

    # 1. Compile
    compile_cmd = f"{d4j_cmd_path} compile"
    proc = subprocess.run(compile_cmd, shell=True, cwd=work_dir, capture_output=True, text=True, errors='replace')
    if proc.returncode != 0:
        error_info["type"] = "compilation"
        error_info["message"] = f"Compilation failed:\n---\n{proc.stderr}\n---"
        return False, error_info
            
    # 2. Run triggering tests
    for test in test_methods:
        test_cmd = f"{d4j_cmd_path} test -t {test.strip()}"
        try:
            proc = subprocess.run(test_cmd, shell=True, cwd=work_dir, capture_output=True, text=True, timeout=180, errors='replace')
            if "Failing tests: 0" not in proc.stdout:
                combined_output = proc.stdout + "\n" + proc.stderr
                method, _ = parse_d4j_test_output(combined_output)
                error_info.update({
                    "type": "trigger_fail",
                    "message": f"Triggering test failed:\n---\n{combined_output.strip()}\n---",
                    "failing_test_method": method
                })
                return False, error_info
        except subprocess.TimeoutExpired:
            error_info["type"] = "timeout"
            error_info["message"] = "Triggering test failed: Timeout expired after 180 seconds."
            return False, error_info
            
    # 3. Run regression tests
    regression_cmd = f"{d4j_cmd_path} test"
    try:
        proc = subprocess.run(regression_cmd, shell=True, cwd=work_dir, capture_output=True, text=True, timeout=300, errors='replace')
        if "Failing tests: 0" not in proc.stdout:
            combined_output = proc.stdout + "\n" + proc.stderr
            method, _ = parse_d4j_test_output(combined_output)
            error_info.update({
                "type": "regression_fail",
                "message": f"Regression test failed:\n---\n{combined_output.strip()}\n---",
                "failing_test_method": method
            })
            return False, error_info
    except subprocess.TimeoutExpired:
        error_info["type"] = "timeout"
        error_info["message"] = "Regression test failed: Timeout expired after 300 seconds."
        return False, error_info

    return True, error_info

def validate_patch(bug_id, patch, bug_data, d4j_cmd_path, loc_folder):
    project, bug_num = bug_id.split("-")
    start, end = bug_data['start'], bug_data['end']
    tmp_work_dir = f"/tmp/test_{bug_id}"
    subprocess.run(f'rm -rf {tmp_work_dir}', shell=True, check=True)
    checkout_cmd = f"{d4j_cmd_path} checkout -p {project} -v {bug_num}b -w {tmp_work_dir}"
    subprocess.run(checkout_cmd, shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    test_methods_output = os.popen(f'{d4j_cmd_path} export -w {tmp_work_dir} -p tests.trigger').read()
    test_methods = test_methods_output.strip().splitlines()
    source_dir = os.popen(f"{d4j_cmd_path} export -p dir.src.classes -w {tmp_work_dir}").read().strip()
    with open(os.path.join(loc_folder, f"{bug_id}.buggy.lines"), "r") as f:
        locs = f.read()
    loc = set([x.split("#")[0] for x in locs.splitlines()]).pop()
    source_file_path = os.path.join(tmp_work_dir, source_dir, loc)
    try:
        with open(source_file_path, 'r', encoding='utf-8') as f:
            source_lines = f.read().split('\n')
    except UnicodeDecodeError:
        with open(source_file_path, 'r', encoding='ISO-8859-1') as f:
            source_lines = f.read().split('\n')
    patch_lines = patch.split('\n')
    new_source = "\n".join(source_lines[:start - 1] + patch_lines + source_lines[end:])
    try:
        with open(source_file_path, 'w', encoding='utf-8') as f:
            f.write(new_source)
    except UnicodeEncodeError:
         with open(source_file_path, 'w', encoding='ISO-8859-1') as f:
            f.write(new_source)
    is_valid, error_info = run_d4j_test(d4j_cmd_path, tmp_work_dir, test_methods)
    subprocess.run(f'rm -rf {tmp_work_dir}', shell=True)
    return is_valid, error_info