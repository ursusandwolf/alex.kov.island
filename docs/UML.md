# Island Simulator Architecture (v1.4)

## Class Diagram Concepts

### Engine Layer
- `SimulationWorld` (Island): Central hub. Uses **Dynamic Partitioning** to calculate optimal `chunkSize` based on core count and grid density.
- `SimulationNode` (Cell): Spatial unit. Uses `ReentrantReadWriteLock` for fine-grained thread safety.
- `GameLoop`: Orchestrates grouped `CellService` execution across world chunks.
- `CellService`: Interface for business logic (Feeding, Movement, etc.) optimized for parallel grouping.
- `SimulationMetrics`: Thread-safe builder for incremental aggregation of population and energy stats.
- `SimulationConstants`: Pure constant registry for `SCALE_1M` and `SCALE_10K` arithmetic.
...
### Domain Layer (Lombok Powered)
- `Organism`: Base for all life. Standardized on `long` energy (fixed-point).
- `Animal` (Herbivore/Predator): LOD 0 entities with individual logic and hibernation support.
- **Special Abilities**: `Fox` (High Agility -60% hunt cost), `Bear` (Hibernation), `Wolf` (Pack Hunting).
- `SwarmOrganism`: LOD 1 entities (Plants, Butterflies) using mass-based aggregation.
- `EntityContainer`: O(1) management using indexed buckets and `LinkedHashSet`.

### Services (The Logic)
- `MovementService`: Coordinate-ordered cell transitions.
- `FeedingService`: Optimized hunting/grazing with pre-calculated interaction matrices.
- `ReproductionService`: Population growth with LOD scaling.
- `LifecycleService`: Aging, metabolism, and seasonal hibernation mechanics.
- `CleanupService`: O(1) removal and pool-based recycling of dead entities.
- `StatisticsService`: Zero-scan reporting using pre-aggregated metrics.

## Core Principles

### 1. Integer-Based Arithmetic
To ensure deterministic results and high performance, the engine uses fixed-point arithmetic:
- `SCALE_1M` (1,000,000) for mass, energy, and consumption.
- `SCALE_10K` (basis points) for growth rates, hunting probabilities, and mutation chances.

### 2. Level of Detail (LOD)
- **LOD 0**: Individual processing for complex animals.
- **LOD 1**: Statistical sampling and swarm aggregation for high-density species (plants, insects).

### 3. Concurrency Model
- **Grouped Parallelism**: Multiple `CellService` tasks are processed per cell in a single parallel pass, minimizing fork-join overhead.
- **Cell-Level Locking**: Services lock only the cells they are working on.
- **Lock Ordering**: To prevent deadlocks during cross-cell movement, cells are always locked in (X, Y) order.
- **Incremental Aggregation**: Metrics are collected during the parallel pass, eliminating global state contention.

### 4. Boilerplate-Free Domain
- Mandatory use of **Lombok** (`@Getter`, `@Setter`, `@Builder`) to keep domain logic clean.
- Removal of redundant Javadocs and FQNs; architectural "know-how" is deferred to `README.md`.
