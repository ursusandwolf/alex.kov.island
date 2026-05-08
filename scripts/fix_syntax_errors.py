import os
import re

def fix_syntax_errors(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Pattern: methodCall(args; -> methodCall(args);
    # This specifically looks for missing ) before ;
    # We need to be careful not to break for loops or other things.
    # Usually it's like .willReturn(obj; or given(obj; or assertEquals(a, b;
    
    # Common patterns in the errors:
    # assertEquals(..., ...;
    # assertTrue(...;
    # assertFalse(...;
    # given(...;
    # verify(...;
    # .willReturn(...;
    # new SpeciesKey(...;
    # new DefaultRandomProvider();
    
    patterns = [
        (r'assertEquals\(([^;)]+);', r'assertEquals(\1);'),
        (r'assertTrue\(([^;)]+);', r'assertTrue(\1);'),
        (r'assertFalse\(([^;)]+);', r'assertFalse(\1);'),
        (r'given\(([^;)]+);', r'given(\1);'),
        (r'verify\(([^;)]+);', r'verify(\1);'),
        (r'\.willReturn\(([^;)]+);', r'.willReturn(\1);'),
        (r'new SpeciesKey\(([^;)]+);', r'new SpeciesKey(\1);'),
        (r'new DefaultRandomProvider\(\);', r'new DefaultRandomProvider();'), # Correcting double ;; later
        (r'new DefaultRandomProvider\(\);;', r'new DefaultRandomProvider());'), # Special case seen in MovementServiceTest
    ]

    new_content = content
    for pattern, replacement in patterns:
        new_content = re.sub(pattern, replacement, new_content)

    # Specific fix for MovementServiceTest style errors
    new_content = new_content.replace('new DefaultEventBus();', 'new DefaultEventBus());')
    new_content = new_content.replace('executor), new DefaultEventBus();', 'executor, randomProvider);')
    
    # Fix double )) if they occurred
    new_content = new_content.replace('));;', ');')
    
    if new_content != content:
        with open(file_path, 'w') as f:
            f.write(new_content)
        return True
    return False

test_dir = 'src/test/java'
fixed_files = []
for root, dirs, files in os.walk(test_dir):
    for file in files:
        if file.endswith('.java'):
            full_path = os.path.join(root, file)
            if fix_syntax_errors(full_path):
                fixed_files.append(full_path)

print(f"Fixed syntax errors in {len(fixed_files)} files.")
