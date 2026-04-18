# Island Ecosystem Simulator

Educational ecosystem simulator demonstrating OOP principles, design patterns (GOF/GRASP), and multithreading in Java.

## Project Structure

```
src/main/java/com/island/
├── content/                    # Content layer (organisms)
│   ├── OrganismBehavior.java   # Interface with default methods
│   ├── Organism.java           # Abstract base class
│   ├── Animal.java             # Abstract animal class
│   ├── Plant.java              # Abstract plant class
│   ├── SpeciesConfig.java      # Singleton configuration
│   ├── AnimalFactory.java      # Factory Method pattern
│   └── animals/                # Concrete animal implementations
│       ├── Wolf.java
│       ├── Rabbit.java
│       └── ... (to be added)
├── model/                      # Model layer (world structure)
│   ├── Cell.java               # Thread-safe cell implementation
│   ├── Chunk.java              # Chunk for multithreading (TODO)
│   ├── ChunkManager.java       # Chunk coordinator (TODO)
│   └── Island.java             # Island map (TODO)
├── engine/                     # Engine layer (simulation logic)
│   ├── SimulationEngine.java   # Main simulation loop (TODO)
│   ├── Statistics.java         # Statistics tracking (TODO)
│   └── SimulatorMain.java      # Entry point (TODO)
└── util/                       # Utilities
    └── ... (TODO)
```

## Design Patterns Used

### GOF Patterns
1. **Template Method** - `Organism`, `Animal`, `Plant` define algorithm skeletons
2. **Factory Method** - `AnimalFactory` creates animals by species key
3. **Singleton** - `SpeciesConfig` provides global configuration
4. **Strategy** - Behavior interfaces allow swapping strategies
5. **Mediator** - `Cell` coordinates organism interactions

### GRASP Principles
1. **Information Expert** - Each class manages its own data
2. **Creator** - `AnimalFactory` creates animals
3. **Controller** - `SimulationEngine` controls simulation flow
4. **High Cohesion** - Related functionality grouped together
5. **Low Coupling** - Minimal dependencies between classes

## Key Features

### Organism Lifecycle
- **Energy System**: 0-100% energy scale
  - ≥30%: Can perform all actions (eat, move, reproduce)
  - 0-30%: Can only eat
  - 0%: Death
- **Action Costs**: Each action costs 5% of max energy
- **Aging**: Organisms age each tick, die at max lifespan (10000 ticks)
- **Reproduction**: Requires 2 individuals, offspring gets 50% parent's energy

### Multithreading Architecture
- **Chunk-based Processing**: Island divided into chunks processed in parallel
- **Thread Pools**:
  - `ScheduledExecutorService`: Main simulation loop, plant growth, statistics
  - `FixedThreadPool`: Parallel chunk processing
- **Thread Safety**: 
  - `ReentrantLock` in `Cell` for write operations
  - `CopyOnWriteArrayList` for organism collections
  - `ThreadLocalRandom` for probability rolls

### Turn Order (Priority System)
1. Fastest animals move first (by speed stat)
2. Equal speed: predators before herbivores
3. Plants are eaten simultaneously (no order)

## TODO List

### Phase 1: Core Content (Current)
- [x] Base organism classes with stubs
- [x] Species configuration singleton
- [x] Wolf and Rabbit example implementations
- [ ] Add remaining 13 animal species
- [ ] Complete probability matrix in SpeciesConfig

### Phase 2: World Model
- [ ] Implement Island class (2D grid of cells)
- [ ] Implement Chunk and ChunkManager
- [ ] Add terrain types (river, forest, plain)
- [ ] Implement initialization (15-45% fill rate)

### Phase 3: Simulation Engine
- [ ] Implement 4-phase turn system:
  1. Eat phase
  2. Move phase  
  3. Reproduce phase
  4. State check phase
- [ ] Add priority-based ordering
- [ ] Implement cross-chunk movement
- [ ] Add statistics collection

### Phase 4: Polish
- [ ] Console visualization
- [ ] Configuration file support
- [ ] Unit tests
- [ ] Documentation

## Building and Running

```bash
# Compile
mvn compile

# Run (when main class is ready)
mvn exec:java -Dexec.mainClass="com.island.engine.SimulatorMain"

# Test
mvn test
```

## Educational Notes

This project is designed for learning:
- Code contains **TODO comments** marking areas for student implementation
- Stubs provide structure but require logic completion
- Comments explain design decisions and pattern usage
- Modular design allows incremental development

## License

Educational project - free to use for learning purposes.
