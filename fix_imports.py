import os
import re

def fix_file(file_path):
    if file_path.endswith('fix_imports.py'):
        return

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Split into lines for initial processing
    lines = content.splitlines()
    imports = []
    body = []
    package_line = None
    
    # Identify package and initial imports
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('package '):
            package_line = line
        elif stripped.startswith('import '):
            # Handle multiple imports on one line
            parts = stripped.split(';')
            for part in parts:
                p = part.strip()
                if p:
                    imports.append(p + ';')
        elif stripped or body:
            body.append(line)

    body_content = "\n".join(body)

    # Constants from SimulationConstants
    simulation_constants = {
        'SCALE_1M', 'SCALE_10K', 'COLD_BLOODED_FEED_INTERVAL', 'COLD_BLOODED_MOVE_INTERVAL',
        'COLD_BLOODED_REPRO_INTERVAL', 'PLANT_GROWTH_RATE_BP', 'PLANT_INITIAL_BIOMASS_BP',
        'HUNT_ROI_THRESHOLD_BP', 'HUNT_STRIKE_COST_MAX_ENERGY_CAP_BP', 'HUNT_STRIKE_COST_PREY_WEIGHT_BP',
        'PREY_RELATIVE_SPEED_HUNT_COST_STEP_BP', 'WOLF_PACK_MAX_BONUS_PERCENT',
        'HERBIVORE_METABOLISM_MODIFIER_BP', 'HERBIVORE_OFFSPRING_BONUS', 'REPTILE_METABOLISM_MODIFIER_BP',
        'BUTTERFLY_REPRODUCTION_RATE_BP', 'CATERPILLAR_FEED_EFFICIENCY_BP', 'CATERPILLAR_METABOLISM_RATE_BP'
    }

    collections_methods = {
        'emptyMap', 'unmodifiableCollection', 'emptyList', 'singletonList', 'singletonMap', 
        'unmodifiableList', 'unmodifiableMap', 'unmodifiableSet', 'emptySet', 'unmodifiableSet'
    }

    # 1. Replace FQNs and class prefixes for SimulationConstants
    body_content = body_content.replace('com.island.config.SimulationConstants.', '')
    if 'class SimulationConstants' not in body_content and 'interface SimulationConstants' not in body_content:
        body_content = body_content.replace('SimulationConstants.', '')

    # 2. Replace FQNs for Collections
    body_content = body_content.replace('java.util.Collections.', 'Collections.')
    
    # 3. Ensure Collections. prefix for common methods if missing
    for method in collections_methods:
        # Use regex to find methods NOT preceded by 'Collections.' and NOT part of a larger word
        # This is a bit aggressive but given the context of the broken project it might be necessary
        pattern = r'(?<!Collections\.)\b' + method + r'\('
        body_content = re.sub(pattern, 'Collections.' + method + '(', body_content)

    # Check usage again after replacements
    use_collections = 'Collections.' in body_content
    use_sim_constants = False
    for const in simulation_constants:
        if re.search(r'\b' + const + r'\b', body_content):
            use_sim_constants = True
            break

    # Clean up and reconstruct imports
    cleaned_imports = set()
    for imp in imports:
        # Remove invalid static imports
        if imp.startswith('import static ') and '.' not in imp:
            continue
        # Remove self-import of SimulationConstants
        if 'SimulationConstants' in file_path and 'import static com.island.config.SimulationConstants.*' in imp:
            continue
        cleaned_imports.add(imp)

    if use_sim_constants and 'SimulationConstants.java' not in file_path:
        cleaned_imports.add('import static com.island.config.SimulationConstants.*;')
    
    if use_collections:
        cleaned_imports.add('import java.util.Collections;')

    # Reconstruct file
    final_output = []
    if package_line:
        final_output.append(package_line)
        final_output.append('')
    
    for imp in sorted(list(cleaned_imports)):
        final_output.append(imp)
    
    if cleaned_imports:
        final_output.append('')
    
    final_output.append(body_content)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(final_output))

def main():
    for base_dir in ['/Users/alex/IdeaProjects/alex.kov.island/src/main/java', '/Users/alex/IdeaProjects/alex.kov.island/src/test/java']:
        if not os.path.exists(base_dir):
            continue
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                if file.endswith('.java'):
                    fix_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
