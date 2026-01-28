# File: collect_plausible_patches.py

import json
import os

def collect_patches(input_file_path, output_file_path):
    """
    Scans a repair result JSON file, collects the first plausible patch for each bug,
    and saves them to a new JSON file.

    A plausible patch is one marked with "valid": true.
    """
    print(f"Reading repair results from: {input_file_path}")

    try:
        with open(input_file_path, "r") as f:
            repair_results = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"Error: Could not read or parse the input file. {e}")
        return

    plausible_patches = {}
    
    print("Starting collection of plausible patches...")
    for bug_id, attempts in repair_results.items():
        if not isinstance(attempts, list):
            print(f"  - Skipping {bug_id}: Invalid format (expected a list of attempts).")
            continue

        found_plausible = False
        for attempt in attempts:
            if isinstance(attempt, dict) and attempt.get('valid') is True:
                # Found the first plausible patch for this bug
                plausible_patches[bug_id] = {
                    "patch": attempt.get('patch', '')
                }
                print(f"  + Found plausible patch for: {bug_id}")
                found_plausible = True
                break # Move to the next bug_id once the first is found
        
        if not found_plausible:
            print(f"  - No plausible patch found for: {bug_id}")

    # Ensure the output directory exists
    output_dir = os.path.dirname(output_file_path)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
    
    # Write the collected patches to the output file
    with open(output_file_path, "w") as f:
        json.dump(plausible_patches, f, indent=2)

    print("\nCollection complete.")
    print(f"Successfully collected {len(plausible_patches)} plausible patches.")
    print(f"Results saved to: {output_file_path}")

if __name__ == "__main__":
    input_file = "repair_result_iterative.json"
    output_file = "plausible_patches.json"

    collect_patches(input_file, output_file)
