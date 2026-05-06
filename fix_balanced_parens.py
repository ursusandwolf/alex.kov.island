import os

def balance_parentheses(line):
    # Only try to fix if it's a line with a method call or assertion and ends with ;
    if ';' not in line or '(' not in line:
        return line
    
    stripped = line.strip()
    if not (stripped.startswith('assert') or stripped.startswith('verify') or stripped.startswith('given') or stripped.startswith('assertEquals') or stripped.startswith('assertTrue')):
         if 'new ' not in stripped:
             return line

    count = 0
    for char in line:
        if char == '(':
            count += 1
        elif char == ')':
            count -= 1
    
    if count > 0:
        # Missing closing parentheses
        parts = line.rsplit(';', 1)
        return parts[0] + (')' * count) + ';' + parts[1]
    
    return line

def fix_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = [balance_parentheses(l) for l in lines]
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

def main():
    for root, dirs, files in os.walk('src/test/java'):
        for file in files:
            if file.endswith('.java'):
                fix_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
