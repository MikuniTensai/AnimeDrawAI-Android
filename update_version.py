import re

# Read the file
with open('app/build.gradle.kts', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace version code and name
content = re.sub(r'versionCode = 20', 'versionCode = 26', content)
content = re.sub(r'versionName = "1\.0\.20 build 20"', 'versionName = "1.0.26"', content)

# Write back
with open('app/build.gradle.kts', 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated version to 26 (1.0.26)")
