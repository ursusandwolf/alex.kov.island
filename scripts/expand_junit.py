import os
import re

ASSERTIONS = [
    "assertEquals", "assertTrue", "assertFalse", "assertNotNull", "assertNull",
    "assertThrows", "assertAll", "assertIterableEquals", "assertArrayEquals",
    "assertSame", "assertNotSame", "fail"
]

def expand_junit_wildcards(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    if 'import static org.junit.jupiter.api.Assertions.*;' not in content:
        return

    used_assertions = []
    for assertion in ASSERTIONS:
        if re.search(rf'\b{assertion}\b', content):
            used_assertions.append(assertion)
    
    if not used_assertions:
        # If none found, maybe it's used in a way I missed, but let's at least keep it compiling
        # Or maybe it's just an unused import
        new_import = ""
    else:
        new_import = "\n".join([f"import static org.junit.jupiter.api.Assertions.{a};" for a in sorted(used_assertions)])

    new_content = content.replace('import static org.junit.jupiter.api.Assertions.*;', new_import)
    
    if new_content != content:
        with open(file_path, 'w') as f:
            f.write(new_content)
        print(f"Expanded JUnit wildcards in {file_path}")

def main():
    for root, dirs, files in os.walk('src/test/java'):
        for file in files:
            if file.endswith('.java'):
                expand_junit_wildcards(os.path.join(root, file))

if __name__ == "__main__":
    main()
