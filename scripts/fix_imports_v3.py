import os
import re

MAPPING = {
    "Animal": "com.island.nature.entities.core",
    "Organism": "com.island.nature.entities.core",
    "Biomass": "com.island.nature.entities.core",
    "GenericAnimal": "com.island.nature.entities.core",
    "GenericBiomass": "com.island.nature.entities.core",
    "SwarmOrganism": "com.island.nature.entities.core",
    "AnimalType": "com.island.nature.entities.core",
    "SizeClass": "com.island.nature.entities.core",
    "DeathCause": "com.island.nature.entities.core",
    "SpeciesKey": "com.island.nature.entities.core",
    "NatureRegistry": "com.island.nature.entities.registry",
    "SpeciesRegistry": "com.island.nature.entities.registry",
    "SpeciesLoader": "com.island.nature.entities.registry",
    "AnimalFactory": "com.island.nature.entities.registry",
    "WorldInitializer": "com.island.nature.entities.registry",
    "BiomassManager": "com.island.nature.entities.registry",
    "HuntingStrategy": "com.island.nature.entities.strategy",
    "DefaultHuntingStrategy": "com.island.nature.entities.strategy",
    "PreyProvider": "com.island.nature.entities.strategy",
    "NatureDomainContext": "com.island.nature.entities.domain",
    "NatureEnvironment": "com.island.nature.entities.domain",
    "NatureWorld": "com.island.nature.entities.domain",
    "SimulationMetrics": "com.island.nature.entities.domain",
    "NatureStatistics": "com.island.nature.entities.domain",
    "TaskRegistry": "com.island.nature.entities.domain",
    "Season": "com.island.nature.entities.environment",
    "SeasonManager": "com.island.nature.entities.environment",
    "SimulationEngine": "com.island.engine.core",
    "SimulationWorld": "com.island.engine.core",
    "SimulationNode": "com.island.engine.core",
    "SimulationPlugin": "com.island.engine.core",
    "SimulationContext": "com.island.engine.core",
    "ExecutionMode": "com.island.engine.core",
    "GameLoop": "com.island.engine.scheduling",
    "PhaseScheduler": "com.island.engine.scheduling",
    "Phase": "com.island.engine.scheduling",
    "ScheduledTask": "com.island.engine.scheduling",
    "ParallelDispatcher": "com.island.engine.parallel",
    "ParallelTask": "com.island.engine.parallel",
    "NodeSnapshot": "com.island.engine.model",
    "WorldSnapshot": "com.island.engine.model",
    "Tickable": "com.island.engine.model",
    "Mortal": "com.island.engine.model",
    "CellService": "com.island.engine.service",
    "GridUtils": "com.island.util.math",
    "SamplingUtils": "com.island.util.sampling",
    "SamplingContext": "com.island.util.sampling",
    "RandomProvider": "com.island.util.common",
    "DefaultRandomProvider": "com.island.util.common",
    "RandomUtils": "com.island.util.common",
    "ObjectPool": "com.island.util.common",
    "Poolable": "com.island.util.common",
    "ViewUtils": "com.island.util.common",
    "InteractionMatrix": "com.island.util.interaction",
    "InteractionProvider": "com.island.util.interaction"
}

REVERSE_MAPPING = {}
for cls, pkg in MAPPING.items():
    fqn = f"{pkg}.{cls}"
    REVERSE_MAPPING[fqn] = cls

def fix_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # 1. Fix package declaration
    rel_path = os.path.relpath(file_path, os.getcwd())
    # Extract package from path
    match = re.search(r'src/(?:main|test)/java/(.+)/[^/]+\.java$', rel_path)
    if not match:
        return
    
    expected_package = match.group(1).replace('/', '.')
    
    new_content = re.sub(r'^package\s+[\w.]+;', f'package {expected_package};', content, count=1, flags=re.MULTILINE)

    # 2. Replace FQNs in code
    for fqn, short in REVERSE_MAPPING.items():
        # Only replace if not in import statement
        # Using a negative lookbehind for 'import '
        new_content = re.sub(rf'(?<!import\s){re.escape(fqn)}', short, new_content)

    # 3. Identify used classes from mapping
    used_classes = set()
    for cls in MAPPING.keys():
        # Match class name as whole word, not in comments or strings (simplified)
        if re.search(rf'\b{cls}\b', new_content):
            # Check if it's not part of an import or package or FQN that we just replaced (though we replaced them already)
            # Actually, if it's in the code, we need an import unless it's in the same package.
            used_classes.add(cls)

    # 4. Remove old imports of these classes
    lines = new_content.splitlines()
    final_lines = []
    current_imports = set()
    
    # Also collect existing imports to see what's already there
    for line in lines:
        import_match = re.match(r'^import\s+([\w.*]+);', line)
        if import_match:
            imp = import_match.group(1)
            # Remove if it's one of our mapped classes (any old version of it)
            should_remove = False
            for cls in MAPPING.keys():
                if imp.endswith(f'.{cls}'):
                    should_remove = True
                    break
            
            # Remove if it's a wildcard import of one of our target packages
            if imp.endswith('.*'):
                pkg = imp[:-2]
                if any(p == pkg for p in MAPPING.values()):
                    should_remove = True
            
            if not should_remove:
                final_lines.append(line)
                current_imports.add(imp)
        else:
            final_lines.append(line)

    # 5. Add new explicit imports
    new_imports = []
    for cls in used_classes:
        target_pkg = MAPPING[cls]
        if target_pkg != expected_package:
            new_imports.append(f"import {target_pkg}.{cls};")

    # Insert new imports after package declaration or existing imports
    package_line_idx = -1
    last_import_idx = -1
    for i, line in enumerate(final_lines):
        if line.startswith('package '):
            package_line_idx = i
        if line.startswith('import '):
            last_import_idx = i

    insertion_idx = -1
    if last_import_idx != -1:
        insertion_idx = last_import_idx + 1
    elif package_line_idx != -1:
        insertion_idx = package_line_idx + 1
    else:
        insertion_idx = 0

    # Filter out duplicates and existing ones
    new_imports = [imp for imp in new_imports if imp.split()[1][:-1] not in current_imports]
    
    if new_imports:
        # Add a blank line before if inserting after package
        if last_import_idx == -1 and package_line_idx != -1:
             final_lines.insert(insertion_idx, "")
             insertion_idx += 1
        
        for imp in sorted(new_imports):
            final_lines.insert(insertion_idx, imp)
            insertion_idx += 1

    # 6. Ensure no wildcard imports (expand if possible, or just remove and let it fail? 
    # The instruction says "Ensure NO wildcard imports are used")
    # I'll just remove them if they match our packages. 
    # If there are other wildcards, I should probably expand them too if I knew where they come from.
    # But let's focus on the mapping first.

    final_content = "\n".join(final_lines)
    
    if final_content != content:
        with open(file_path, 'w') as f:
            f.write(final_content)
        print(f"Fixed {file_path}")

def main():
    java_files = []
    for root, dirs, files in os.walk('src'):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    
    for file in java_files:
        fix_file(file)

if __name__ == "__main__":
    main()
