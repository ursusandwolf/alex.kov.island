# Island Ecosystem Simulator

Educational ecosystem simulator demonstrating OOP principles, design patterns (GOF/GRASP), and high-performance multithreading in Java 21.

## Architecture & Design Patterns

### Modern Architecture (Java 21)
- **Virtual Threads (Project Loom)**: High-scalability parallel processing using `VirtualThreadPerTaskExecutor`.
- **Dependency Injection (DI)**: Core services (Feeding, Reproduction) utilize constructor injection for strategies and registries, ensuring high testability.
- **Deterministic Simulation**: Pluggable `RandomProvider` architecture allows for 100% reproducible simulation runs via fixed seeds.

### GOF Patterns
1. **Template Method** - `Organism`, `Animal`, `Biomass` define behavior skeletons.
2. **Factory Method** - `AnimalFactory` creates organisms by species key.
3. **Strategy** - `HuntingStrategy` allows swapping predator behavior logic.
4. **Flyweight** - `AnimalType` stores common species data shared by thousands of instances.
5. **Mediator** - `Cell` coordinates atomic interactions between organisms.

### Performance Optimizations
- **Smart Biomass**: Plants and small insects are modeled as mass containers instead of individual objects, reducing heap pressure by millions of objects.
- **O(1) Spatial Indexing**: `Cell` uses `EnumMap` and role-based buckets for constant-time access to specific species or size classes.
- **High-Perf Interaction Matrix**: Uses primitive `int[][]` arrays and ordinal indexing for maximum cache locality and zero-boxing overhead.
- **Fast Removal**: `Cell` implements *swap-to-remove* logic for $O(1)$ animal removal during death or movement.

## Key Features

### High-Performance Multithreading
- **Chunk-based Processing**: The island is divided into chunks processed in parallel using Virtual Threads.
- **Thread Safety**: 
  - Fine-grained `ReentrantLock` per `Cell`.
  - Deterministic locking order (by coordinates) to prevent deadlocks during movement.
  - Safe snapshots for concurrent iteration.

### Biological Model
- **Kleiber’s Law**: Metamorphic energy costs scaled by organism size class.
- **ROI-Driven Hunting**: Predators evaluate the energy "Return on Investment" before hunting, considering success rate, strike effort, and chase costs.
- **Hunt Fatigue**: Progressive energy exhaustion for failed hunting attempts.
- **Red Book Protection**: Automatic stealth and reproduction bonuses for endangered species to ensure ecosystem stability.

## Development & Testing

### Running Tests
```bash
# Run full suite
mvn test

# Run specific performance tests
mvn test -Dtest=ReproducibilityTest,StabilityIntegrationTest
```

### Static Analysis
```bash
# Checkstyle (Google Style Guide)
mvn checkstyle:check
```

## Emergency & Rollback Plan
In case of critical regression or performance degradation:
1. **Revert Merge**: `git revert -m 1 <merge_commit>`
2. **Disable Virtual Threads**: Switch to `FixedThreadPool` in `GameLoop.java`.
3. **Toggle Protection**: Disable "Red Book" protection in `SimulationConstants.java` if stability is compromised.

## License
Educational project - free to use for learning purposes.
