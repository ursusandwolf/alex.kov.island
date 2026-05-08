import os
import re

def fix_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    lines = content.split('\n')
    new_lines = []
    imports = set()
    static_imports = set()
    package = ""
    
    # Simple regex to find FQNs starting with com.island
    fqn_pattern = re.compile(r'com\.island\.[\w.]+')

    body_started = False
    for line in lines:
        if line.startswith('package '):
            package = line.split(' ')[1].strip(';')
            continue
        if line.startswith('import '):
            parts = line.split(';')
            for part in parts:
                p = part.strip()
                if p.startswith('import '):
                    if ' static ' in p:
                        static_imports.add(p + ';')
                    else:
                        imports.add(p + ';')
            continue
        
        if not body_started and line.strip() and not line.startswith('package') and not line.startswith('import'):
            body_started = True
        
        if body_started:
            # In code body, find FQNs
            matches = fqn_pattern.findall(line)
            new_line = line
            for fqn in matches:
                parts = fqn.split('.')
                class_index = -1
                for i, part in enumerate(parts):
                    if part and part[0].isupper():
                        class_index = i
                        break
                
                if class_index != -1:
                    class_fqn = '.'.join(parts[:class_index+1])
                    simple_name = parts[class_index]
                    
                    # Avoid replacing parts of larger FQNs incorrectly
                    # Only replace if it matches class_fqn followed by dot or non-word char
                    pattern = re.compile(re.escape(class_fqn) + r'(\.|\b)')
                    new_line = pattern.sub(simple_name + r'\1', new_line)
                    
                    imports.add(f"import {class_fqn};")
            new_lines.append(new_line)

    # Reconstruct the file
    final_output = [f"package {package};", ""]
    
    if static_imports:
        for imp in sorted(list(static_imports)):
            final_output.append(imp)
        final_output.append("")
        
    if imports:
        # Filter imports from the same package
        filtered_imports = []
        for imp in sorted(list(imports)):
            imp_path = imp.replace('import ', '').replace(';', '').strip()
            imp_package = imp_path.rsplit('.', 1)[0]
            if imp_package != package:
                filtered_imports.append(imp)
        
        for imp in filtered_imports:
            final_output.append(imp)
        final_output.append("")
            
    # Remove leading empty lines from body
    while new_lines and not new_lines[0].strip():
        new_lines.pop(0)
        
    final_output.extend(new_lines)

    with open(file_path, 'w') as f:
        f.write('\n'.join(final_output))

def main():
    for base_dir in ['src/main/java', 'src/test/java']:
        if not os.path.exists(base_dir):
            continue
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                if file.endswith('.java'):
                    fix_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
