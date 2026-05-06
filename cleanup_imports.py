import os
import re

def cleanup_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    if not content:
        return

    # Find package
    package_match = re.search(r"package\s+([\w.]+);", content)
    package = package_match.group(1) if package_match else None
            
    # Find all imports
    imports = set(re.findall(r"import\s+([\w.]+);", content))

    # filter imports
    cleaned_imports = set()
    for imp in imports:
        parts = imp.split('.')
        if len(parts) == 1:
            continue
        
        imp_package = ".".join(parts[:-1])
        if imp_package == package:
            continue
            
        cleaned_imports.add(imp)

    # Remove all package and import lines to start clean
    content = re.sub(r"package\s+[\w.]+;\s*", "", content)
    content = re.sub(r"import\s+[\w.]+;\s*", "", content)
    
    # Strip leading/trailing whitespace
    content = content.strip()

    result = []
    if package:
        result.append(f"package {package};")
        result.append("")
        
    if cleaned_imports:
        for imp in sorted(list(cleaned_imports)):
            result.append(f"import {imp};")
        result.append("")
        
    result.append(content)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(result) + "\n")

for root, dirs, files in os.walk("src"):
    for file in files:
        if file.endswith(".java"):
            cleanup_file(os.path.join(root, file))
