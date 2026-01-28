# utils/parse_d4j.py

import json
import difflib

# d4j_bug_lists
d4j_bug_lists = '''
| Chart           | jfreechart                 |       26       | 1-26                | None                    |
| Cli             | commons-cli                |       39       | 1-5,7-40            | 6                       |
| Closure         | closure-compiler           |      174       | 1-62,64-92,94-176   | 63,93                   |
| Codec           | commons-codec              |       18       | 1-18                | None                    |
| Collections     | commons-collections        |        4       | 25-28               | 1-24                    |
| Compress        | commons-compress           |       47       | 1-47                | None                    |
| Csv             | commons-csv                |       16       | 1-16                | None                    |
| Gson            | gson                       |       18       | 1-18                | None                    |
| JacksonCore     | jackson-core               |       26       | 1-26                | None                    |
| JacksonDatabind | jackson-databind           |      112       | 1-112               | None                    |
| JacksonXml      | jackson-dataformat-xml     |        6       | 1-6                 | None                    |
| Jsoup           | jsoup                      |       93       | 1-93                | None                    |
| JxPath          | commons-jxpath             |       22       | 1-22                | None                    |
| Lang            | commons-lang               |       64       | 1,3-65              | 2                       |
| Math            | commons-math               |      106       | 1-106               | None                    |
| Mockito         | mockito                    |       38       | 1-38                | None                    |
| Time            | joda-time                  |       26       | 1-20,22-27          | 21                      |'''


def clean_parse_d4j(file_path):
    """
    Loads, cleans, and parses Defects4J bug data from the specified JSON file.
    """
    with open(file_path, "r") as f:
        result = json.load(f)
    
    cleaned_result = {}
    for k, v in result.items():
        # Clean indentation of buggy function
        buggy_lines = v['buggy'].splitlines()
        if not buggy_lines: continue
        leading_white_space = len(buggy_lines[0]) - len(buggy_lines[0].lstrip())
        cleaned_buggy = "\n".join([line[leading_white_space:] for line in buggy_lines])
        
        # Clean indentation of fix function
        fix_lines = v['fix'].splitlines()
        if not fix_lines: continue
        leading_white_space = len(fix_lines[0]) - len(fix_lines[0].lstrip())
        cleaned_fix = "\n".join([line[leading_white_space:] for line in fix_lines])
        
        # Calculate relative line numbers
        relative_locations = [location - v['start'] + 1 for location in v["location"]]

        key_with_ext = k if k.endswith(".java") else k + ".java"
        
        cleaned_result[key_with_ext] = {
            "buggy": cleaned_buggy,
            "fix": cleaned_fix,
            "location": relative_locations,
            "start": v['start'],  # Preserve the start line number
            "end": v['end']        # Preserve the end line number
        }
    return cleaned_result

def get_unified_diff(original, new):
    """Generates a unified diff between two code strings."""
    original_lines = original.splitlines(keepends=True)
    new_lines = new.splitlines(keepends=True)
    diff = difflib.unified_diff(original_lines, new_lines, fromfile='original', tofile='new')
    return ''.join(diff)