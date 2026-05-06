import os
import re

def fix_test_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    for line in lines:
        # Fix ");" and "));" issues
        # Rule: if we have "), " followed by another argument, it should be ", "
        # Rule: if we have "));" at the end, it might be correct if it's a nested call, 
        # but if it's just "assertNotNull(obj.get());", it should not be "assertNotNull(obj.get();"
        
        l = line
        # Fix assertNotNull(caughtEvent.get(); -> assertNotNull(caughtEvent.get());
        l = re.sub(r'(assert\w+\(.+?\.get\(\))([^)]*);', r'\1\2);', l)
        # Fix assertEquals(..., caughtEvent.get().getCause(); -> assertEquals(..., caughtEvent.get().getCause());
        l = re.sub(r'(assertEquals\(.+?\.get\(\)\.\w+\(\))([^)]*);', r'\1\2);', l)
        # Fix new DefaultEventBus()); -> new DefaultEventBus();
        l = l.replace('DefaultEventBus());', 'DefaultEventBus();')
        # Fix rabbit.getComponent(MovementComponent.class); -> rabbit.getComponent(MovementComponent.class));
        # Actually, many assertions are missing the closing parenthesis for the assertion itself.
        
        # Pattern: assertXXX(something; -> assertXXX(something);
        l = re.sub(r'(assert\w+\([^;)]+);', r'\1);', l)
        
        # Fix speed() -> speed());
        l = re.sub(r'(assertEquals\([^;)]+\.getSpeed\(\));', r'\1);', l)
        
        new_lines.append(l)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

def main():
    for root, dirs, files in os.walk('src/test/java'):
        for file in files:
            if file.endswith('.java'):
                fix_test_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
