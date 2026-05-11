import os
import re

package_map = {
    # com.island.nature.entities
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
    
    # com.island.engine
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
    
    # com.island.util
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

# Add other classes that might be used as FQNs but weren't moved in this batch
# We need to know where they are.
# Let's find all com.island classes first.

all_classes = {}

def find_all_classes():
    for root, dirs, files in os.walk("src/main/java"):
        for file in files:
            if file.endswith(".java"):
                class_name = file[:-5]
                rel_path = os.path.relpath(root, "src/main/java")
                package = rel_path.replace(os.sep, ".")
                all_classes[class_name] = package

find_all_classes()
# Override with our new mapping
all_classes.update(package_map)

def get_package_from_path(file_path):
    if "src/main/java/" in file_path:
        rel_path = os.path.dirname(os.path.relpath(file_path, "src/main/java/"))
    elif "src/test/java/" in file_path:
        rel_path = os.path.dirname(os.path.relpath(file_path, "src/test/java/"))
    else:
        return None
    return rel_path.replace(os.sep, ".")

def fix_file(file_path):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    new_package = get_package_from_path(file_path)
    if not new_package:
        return

    content = "".join(lines)
    
    # Update package declaration
    content = re.sub(r"package com\.island\.[^;]+;", f"package {new_package};", content)
    # If it's com.island (e.g. Main.java), it might just be package com.island;
    content = re.sub(r"package com\.island;", f"package {new_package};", content)

    # Handle FQNs and collect necessary imports
    needed_imports = set()
    
    def fqn_replacer(match):
        fqn = match.group(0)
        parts = fqn.split('.')
        class_name = parts[-1]
        package = ".".join(parts[:-1])
        
        # If it's a known class, we can potentially simplify it
        if class_name in all_classes:
            target_package = all_classes[class_name]
            if target_package != new_package:
                needed_imports.add(f"{target_package}.{class_name}")
            return class_name
        return fqn

    # Replace com.island.X.Y.ClassName with ClassName
    content = re.sub(r"com\.island\.[a-zA-Z0-9_.]+\.([A-Z][a-zA-Z0-9_]+)", fqn_replacer, content)

    # Update existing imports
    def import_replacer(match):
        imp = match.group(1)
        parts = imp.split('.')
        class_name = parts[-1]
        if class_name in all_classes:
            target_package = all_classes[class_name]
            if target_package != new_package:
                needed_imports.add(f"{target_package}.{class_name}")
            return "" # Remove old import, we'll re-add it
        return match.group(0)

    content = re.sub(r"import (com\.island\.[^;]+);", import_replacer, content)

    # Now re-add all needed imports
    lines = content.splitlines()
    final_lines = []
    package_line_idx = -1
    last_import_idx = -1
    
    for i, line in enumerate(lines):
        if line.startswith("package "):
            package_line_idx = i
        if line.startswith("import "):
            last_import_idx = i
            
    # Remove empty import lines left by our replacer
    lines = [l for l in lines if l.strip() != "import ;" and l.strip() != "import"]
    
    # Collect imports again
    existing_imports = set()
    other_lines = []
    for line in lines:
        if line.startswith("import "):
            existing_imports.add(line)
        else:
            other_lines.append(line)
            
    for imp in needed_imports:
        existing_imports.add(f"import {imp};")
        
    # Sort and place imports
    sorted_imports = sorted(list(existing_imports))
    
    # Find where to insert
    new_content = []
    found_package = False
    for line in other_lines:
        new_content.append(line)
        if line.startswith("package "):
            new_content.append("")
            new_content.extend(sorted_imports)
            found_package = True
            
    if not found_package:
         new_content = sorted_imports + [""] + other_lines

    with open(file_path, 'w') as f:
        f.write("\n".join(new_content) + "\n")

for root, dirs, files in os.walk("src"):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))
