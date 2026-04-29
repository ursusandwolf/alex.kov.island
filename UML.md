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
