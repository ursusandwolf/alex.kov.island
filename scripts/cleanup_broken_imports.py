import os
import re

def clean_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    for line in lines:
        # Match "import Name;" but NOT "import static ..." and NOT "import com.something..."
        match = re.match(r'^import\s+([a-zA-Z0-9_]+)\s*;\s*$', line.strip())
        if match:
            continue
        new_lines.append(line)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

def main():
    for root, dirs, files in os.walk('src'):
        for file in files:
            if file.endswith('.java'):
                clean_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
