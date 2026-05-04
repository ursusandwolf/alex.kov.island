import os
import re
from pathlib import Path

# Mapping of package -> set of class names or class -> set of static members
package_map = {}

def populate_package_map():
    # 1. Internal classes from glob results (approximate)
    src_root = Path("/Users/alex/IdeaProjects/alex.kov.island/src/main/java")
    for java_file in src_root.rglob("*.java"):
        # Get package name from path
        rel_path = java_file.relative_to(src_root)
        package = ".".join(rel_path.parent.parts)
        if package:
            package = "com.island." + package if not package.startswith("com.island") else package
        else:
            package = "com.island"
        
        class_name = java_file.stem
        if package not in package_map:
            package_map[package] = set()
        package_map[package].add(class_name)

    # 2. SimulationConstants static members
    constants_file = src_root / "com/island/config/SimulationConstants.java"
    if constants_file.exists():
        content = constants_file.read_text()
        members = re.findall(r"public static final \w+ (\w+)", content)
        package_map["static com.island.config.SimulationConstants"] = set(members)

    # 3. java.util common classes
    package_map["java.util"] = {
        "List", "ArrayList", "LinkedList", "Map", "HashMap", "TreeMap", "LinkedHashMap",
        "Set", "HashSet", "TreeSet", "LinkedHashSet", "Collections", "Arrays", "Objects",
        "Optional", "Scanner", "UUID", "Random", "Iterator", "Enumeration", "Vector",
        "Stack", "Queue", "Deque", "PriorityQueue", "BitSet", "Calendar", "Date",
        "GregorianCalendar", "StringTokenizer", "Properties", "Comparator", "InputMismatchException",
        "NoSuchElementException", "ConcurrentModificationException", "Timer", "TimerTask",
        "Formatter", "Locale", "ResourceBundle", "TimeZone", "ServiceLoader", "Collection"
    }
    
    # 3b. java.io and java.nio
    package_map["java.io"] = {
        "File", "InputStream", "OutputStream", "FileInputStream", "FileOutputStream",
        "BufferedReader", "BufferedWriter", "InputStreamReader", "OutputStreamWriter",
        "FileReader", "FileWriter", "PrintWriter", "IOException", "Serializable",
        "ByteArrayInputStream", "ByteArrayOutputStream", "ObjectInputStream", "ObjectOutputStream",
        "PrintStream", "StringReader", "StringWriter"
    }
    package_map["java.nio.file"] = {
        "Path", "Paths", "Files", "StandardOpenOption", "FileSystems"
    }
    
    # 4. org.junit.jupiter.api.Assertions
    package_map["static org.junit.jupiter.api.Assertions"] = {
        "assertEquals", "assertNotEquals", "assertTrue", "assertFalse", "assertNull", 
        "assertNotNull", "assertSame", "assertNotSame", "assertThrows", "assertDoesNotThrow",
        "assertTimeout", "assertTimeoutPreemptively", "assertAll", "fail"
    }

    # 5. org.mockito.Mockito
    package_map["static org.mockito.Mockito"] = {
        "mock", "when", "verify", "times", "never", "atLeast", "atLeastOnce", "atMost",
        "any", "anyInt", "anyString", "anyLong", "anyList", "eq", "doReturn", "doThrow",
        "doAnswer", "doNothing", "spy", "argumentCaptor", "verifyNoInteractions", "verifyNoMoreInteractions"
    }

def fix_imports(file_path):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    content = "".join(lines)
    
    # Find all wildcard imports
    wildcard_imports = re.findall(r"import (?:static )?([\w\.]+)\.\*;", content)
    if not wildcard_imports:
        return False

    new_imports = set()
    wildcard_lines = []
    
    # Get all words in the file to check for usage
    words = set(re.findall(r"\b\w+\b", content))

    for imp in wildcard_imports:
        # Check if it's static
        is_static = f"import static {imp}.*;" in content
        key = ("static " if is_static else "") + imp
        
        if key in package_map:
            for member in package_map[key]:
                if member in words:
                    # Check if it's not the class definition itself or already explicitly imported
                    # (Simple heuristic)
                    if is_static:
                        new_imports.add(f"import static {imp}.{member};")
                    else:
                        new_imports.add(f"import {imp}.{member};")
        
        # Mark lines for removal
        if is_static:
            wildcard_lines.append(f"import static {imp}.*;\n")
        else:
            wildcard_lines.append(f"import {imp}.*;\n")

    if not new_imports and not wildcard_lines:
        return False

    # Filter out existing explicit imports from new_imports to avoid duplicates
    existing_imports = set(re.findall(r"import (?:static )?[\w\.]+;", content))
    new_imports = {ni for ni in new_imports if ni.strip(";") not in existing_imports}

    # Reconstruct the file
    new_lines = []
    import_inserted = False
    
    # Simple strategy: remove wildcard lines, find first import line, insert new ones there
    # then sort all imports.
    
    current_imports = []
    other_lines = []
    
    in_imports = False
    for line in lines:
        if line.startswith("import "):
            if line not in wildcard_lines:
                current_imports.append(line)
            in_imports = True
        elif line.strip() == "" and in_imports:
            # Keep empty lines between import groups for now? 
            # Actually let's just collect all imports and re-emit them.
            pass
        elif line.startswith("package "):
            other_lines.append(line)
        else:
            other_lines.append(line)
            if line.strip() != "":
                in_imports = False

    all_imports = list(set(current_imports) | {ni + "\n" for ni in new_imports})
    
    # Sort imports: static first, then by name
    all_imports.sort(key=lambda x: (not x.startswith("import static"), x))

    # Build new file content
    final_lines = []
    for line in other_lines:
        final_lines.append(line)
        if line.startswith("package "):
            final_lines.append("\n")
            final_lines.extend(all_imports)
            import_inserted = True
            
    if not import_inserted:
        # No package declaration?
        final_lines = all_imports + ["\n"] + other_lines

    # Clean up multiple newlines
    final_content = "".join(final_lines)
    final_content = re.sub(r'\n{3,}', '\n\n', final_content)

    if final_content != content:
        with open(file_path, 'w') as f:
            f.write(final_content)
        return True
    return False

if __name__ == "__main__":
    populate_package_map()
    root_dirs = [
        "/Users/alex/IdeaProjects/alex.kov.island/src/main/java",
        "/Users/alex/IdeaProjects/alex.kov.island/src/test/java"
    ]
    
    count = 0
    for root_dir in root_dirs:
        for p in Path(root_dir).rglob("*.java"):
            if fix_imports(p):
                print(f"Fixed {p}")
                count += 1
    print(f"Total files fixed: {count}")
