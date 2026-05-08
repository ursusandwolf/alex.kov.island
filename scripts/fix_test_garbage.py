import os
import re

def fix_test_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Fix ReproductionService: new ReproductionService(world, factory, registry), random, eventBus) 
    # -> new ReproductionService(world, factory, registry, executor, random)
    # The garbage produced was often like: new Service(a, b, c), d, e)
    content = re.sub(r'new ReproductionService\(([^,]+),\s*([^,]+),\s*([^,)]+)\),\s*([^,]+),\s*([^,)]+)\)', 
                     r'new ReproductionService(\1, \2, \3, executor, \4)', content)
    
    # 2. Fix MovementService
    content = re.sub(r'new MovementService\(([^,]+),\s*([^,)]+)\),\s*([^,]+),\s*([^,)]+)\)', 
                     r'new MovementService(\1, \2, executor, \3)', content)
    
    # 3. Fix FeedingService
    content = re.sub(r'new FeedingService\(([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\),\s*([^,]+),\s*([^,)]+)\)', 
                     r'new FeedingService(\1, \2, \3, \4, \5, executor, \6)', content)

    # 4. Fix LifecycleService
    content = re.sub(r'new LifecycleService\(([^,]+)\),\s*([^,]+),\s*([^,)]+)\)', 
                     r'new LifecycleService(\1, executor, \2)', content)

    # General cleanup for any remaining garbage like "), random, bus)"
    content = re.sub(r'\),\s*new DefaultRandomProvider\(\),\s*new DefaultEventBus\(\)\)', 
                     r', executor, new DefaultRandomProvider())', content)
    content = re.sub(r'\),\s*random,\s*eventBus\)', 
                     r', executor, random)', content)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    for root, dirs, files in os.walk('src/test/java'):
        for file in files:
            if file.endswith('.java'):
                fix_test_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
