# Island Simulator Architecture (v1.1)

## Class Diagram Concepts

### Engine Layer
- `SimulationWorld` (Island): Central hub.
- `SimulationNode` (Cell): Spatial unit.
- `AbstractService`: Parallel task processor.
- `WorldInitializer`: Setup and bootstrap.

### Domain Layer
- `Organism`: Base for all life.
- `Animal` (Herbivore/Predator): Complex life with energy and metabolism.
- `Biomass` (Plants/Insects): Simple mass-based life.
- `SpeciesRegistry`: Metadata for all species.

### Services (The Logic)
- `MovementService`: Handles spatial transitions.
- `FeedingService`: Handles hunting and grazing.
- `ReproductionService`: Handles population growth.
- `LifecycleService`: Aging and metabolism.
- `CleanupService`: Recycles dead organisms to pools.

## How It Works: The Execution Loop

1. **Bootstrap**: `WorldInitializer` creates the `Island`, populates it using `AnimalFactory` and `SpeciesRegistry`, and injects services.
2. **Game Loop**: `GameLoop` triggers a `tick()` on all services in a predefined sequence:
    - `LifecycleService`: Subtracts energy (metabolism) and increments age.
    - `FeedingService`: Predators hunt, herbivores graze.
    - `MovementService`: Animals move to neighboring cells.
    - `ReproductionService`: Mature animals produce offspring.
    - `CleanupService`: Final pass to remove dead entities and recycle them to pools.
3. **Optimized Processing**:
    - Every service uses `AbstractService.tick()`, which submits tasks to a **Virtual Thread Executor**.
    - **Skip-Tick logic** in `FeedingService` and `MovementService` ensures cold-blooded animals consume less CPU.
    - **LOD logic** ensures that "super-cells" (overcrowded) don't bottleneck the simulation by processing only a statistical sample.
4. **Energy Economy**: Every action (moving, hunting, reproducing) costs energy. Energy is replenished by eating. If energy hits zero, the organism is marked for death and recycled.

## Optimization Strategy

### 1. Object Pooling
`AnimalFactory` maintains a pool of `Animal` objects. When an animal dies, it's not GC'd but sent back to the pool for reuse.

### 2. Tick Scheduler (LOD & Skip Ticks)
- **LOD**: If a cell has >100 animals, only a sample is processed.
- **Skip Ticks**: Cold-blooded species (e.g., SNAKE) act every 2nd or 3rd tick.

### 3. Spatial Indexing
Cells store a pre-calculated list of `neighbors` for O(1) movement lookups.

### 4. Concurrency
Simulation uses **Java 21 Virtual Threads**. Work is divided into independent units (rows/chunks) to minimize lock contention.
