import os
import re

package_map = {
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
    "InteractionProvider": "com.island.util.interaction",
    "DefaultEventBus": "com.island.engine.event",
    "EventBus": "com.island.engine.event",
    "EntityBornEvent": "com.island.engine.event",
    "EntityDiedEvent": "com.island.engine.event",
    "Cell": "com.island.nature.model",
    "Chunk": "com.island.nature.model",
    "Island": "com.island.nature.model",
    "DefaultBiomassManager": "com.island.nature.model",
    "FeedingService": "com.island.nature.service",
    "MovementService": "com.island.nature.service",
    "ReproductionService": "com.island.nature.service",
    "LifecycleService": "com.island.nature.service",
    "StatisticsService": "com.island.nature.service",
    "ProtectionService": "com.island.nature.service",
    "DefaultProtectionService": "com.island.nature.service",
    "AbstractService": "com.island.nature.service",
    "AlertService": "com.island.nature.service",
    "CleanupService": "com.island.nature.service",
    "AgeComponent": "com.island.nature.entities.components",
    "HealthComponent": "com.island.nature.entities.components",
    "MovementComponent": "com.island.nature.entities.components",
}

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
        content = f.read()

    new_package = get_package_from_path(file_path)
    if not new_package:
        return

    # Update package declaration
    content = re.sub(r"package com\.island[^;]*;", f"package {new_package};", content)

    needed_imports = set()
    
    # 1. Update service constructors: remove last parameter if it is an eventBus
    # FeedingService, MovementService, ReproductionService, LifecycleService
    # They now have one less parameter (eventBus removed).
    # We look for new FeedingService(...) etc.
    
    # FeedingService: (world, factory, matrix, registry, strategy, executor, random, eventBus) -> remove eventBus
    content = re.sub(r"(new FeedingService\([^)]+),\s*[^,)]+\)", r"\1)", content)
    # MovementService: (world, registry, executor, random, eventBus) -> remove eventBus
    content = re.sub(r"(new MovementService\([^)]+),\s*[^,)]+\)", r"\1)", content)
    # ReproductionService: (world, factory, registry, executor, random, eventBus) -> remove eventBus
    content = re.sub(r"(new ReproductionService\([^)]+),\s*[^,)]+\)", r"\1)", content)
    # LifecycleService: (world, executor, random, eventBus) -> remove eventBus
    content = re.sub(r"(new LifecycleService\([^)]+),\s*[^,)]+\)", r"\1)", content)

    # 2. Replace FQNs with simple names and collect imports
    def fqn_replacer(match):
        fqn = match.group(0)
        parts = fqn.split('.')
        class_name = parts[-1]
        if class_name in all_classes:
            target_package = all_classes[class_name]
            if target_package != new_package:
                needed_imports.add(f"{target_package}.{class_name}")
            return class_name
        return fqn

    content = re.sub(r"com\.island\.[a-zA-Z0-9_.]+\.([A-Z][a-zA-Z0-9_]+)", fqn_replacer, content)

    # 3. Update existing imports
    def import_replacer(match):
        imp = match.group(1)
        parts = imp.split('.')
        class_name = parts[-1]
        if class_name in all_classes:
            target_package = all_classes[class_name]
            if target_package != new_package:
                needed_imports.add(f"{target_package}.{class_name}")
            return ""
        return match.group(0)

    content = re.sub(r"import (com\.island\.[^;]+);", import_replacer, content)

    # 4. Handle Assertions
    if "assertEquals" in content or "assertTrue" in content or "assertFalse" in content or "assertNotNull" in content:
        if "org.junit.jupiter.api.Assertions" not in content and "static org.junit.jupiter.api.Assertions" not in content:
             needed_imports.add("static org.junit.jupiter.api.Assertions.*")

    lines = content.splitlines()
    # Remove empty lines that were imports
    lines = [l for l in lines if not re.match(r"^\s*import\s*;\s*$", l)]
    
    existing_imports = set()
    other_lines = []
    
    # Remove existing com.island imports to rebuild them
    for line in lines:
        if line.startswith("import ") and "com.island" in line:
            continue
        if line.startswith("import "):
            existing_imports.add(line)
        else:
            other_lines.append(line)
            
    for imp in needed_imports:
        if imp.startswith("static "):
            existing_imports.add(f"import {imp};")
        else:
            existing_imports.add(f"import {imp};")
        
    sorted_imports = sorted(list(existing_imports))
    
    new_content = []
    found_package = False
    for line in other_lines:
        new_content.append(line)
        if line.startswith("package "):
            new_content.append("")
            new_content.extend(sorted_imports)
            found_package = True
            
    if not found_package and sorted_imports:
         new_content = sorted_imports + [""] + other_lines

    final_result = "\n".join(new_content) + "\n"
    # Clean up double empty lines
    final_result = re.sub(r"\n\n\n+", "\n\n", final_result)

    with open(file_path, 'w') as f:
        f.write(final_result)

for root, dirs, files in os.walk("src"):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))
