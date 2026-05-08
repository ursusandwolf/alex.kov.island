import os
import re

predators = {"WOLF", "BOA", "FOX", "BEAR", "EAGLE"}
herbivores_and_plants = {"HORSE", "DEER", "RABBIT", "MOUSE", "HAMSTER", "GOAT", "SHEEP", "BOAR", "BUFFALO", "DUCK", "FROG", "CHAMELEON", "CATERPILLAR", "BUTTERFLY", "PLANT", "GRASS", "MUSHROOM"}

def fix_file(path):
    with open(path, 'r') as f:
        content = f.read()
    
    orig = content
    for p in predators:
        content = content.replace(f"SpeciesKey.{p}", f'new SpeciesKey("{p.lower()}", true)')
    for h in herbivores_and_plants:
        content = content.replace(f"SpeciesKey.{h}", f'new SpeciesKey("{h.lower()}", false)')

    if orig != content:
        with open(path, 'w') as f:
            f.write(content)
        print(f"Fixed {path}")

for root, _, files in os.walk("src/test/java"):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))
