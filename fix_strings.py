import os
import re

files_to_fix = [
    r"app\src\main\res\values-it\strings.xml",
    r"app\src\main\res\values-tr\strings.xml",
    r"app\src\main\res\values-ru\strings.xml",
    r"app\src\main\res\values-vi\strings.xml"
]

def fix_file(filepath):
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        return

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Regex to find apostrophes not preceded by backslash
    # We also need to be careful not to escape apostrophes inside tags like <string name="...">
    # But usually attributes use double quotes, so it should be fine if we just target the whole file 
    # and then fix any double-escapes if necessary, or just be smart.
    # Actually, simplistic replacement might break attributes if they used single quotes, but Android xml usually uses double quotes for attributes.
    
    # Better regex: (?<!\\)'
    new_content = re.sub(r"(?<!\\)'", r"\'", content)

    if content != new_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed {filepath}")
    else:
        print(f"No changes for {filepath}")

for f in files_to_fix:
    fix_file(os.path.join(os.getcwd(), f))
