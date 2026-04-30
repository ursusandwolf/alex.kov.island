# Island Simulator Architecture (v1.2)

## Class Diagram Concepts

### Engine Layer
- `SimulationWorld` (Island): Central hub. Enforces X-Y locking order for dead-lock free concurrency.
- `SimulationNode` (Cell): Spatial unit. Uses `ReentrantReadWriteLock` for fine-grained thread safety.
- `AbstractService`: Parallel task processor using Java 21 Virtual Threads.
- `SimulationConstants`: Pure constant registry for `SCALE_1M` and `SCALE_10K` arithmetic.

### Domain Layer (Lombok Powered)
- `Organism`: Base for all life. Standardized on `long` energy (fixed-point).
- `Animal` (Herbivore/Predator): LOD 0 entities with individual logic.
- `SwarmOrganism`: LOD 1 entities (Plants, Butterflies) using mass-based aggregation.
- `EntityContainer`: O(1) management using indexed buckets and `LinkedHashSet`.

### Services (The Logic)
- `MovementService`: Coordinate-ordered cell transitions.
- `FeedingService`: Optimized hunting/grazing with pre-calculated interaction matrices.
- `ReproductionService`: Population growth with LOD scaling.
- `LifecycleService`: Aging, metabolism, and seasonal cycle integration.
- `CleanupService`: Pool-based recycling of dead entities.

## Core Principles

### 1. Integer-Based Arithmetic
To ensure deterministic results and high performance, the engine uses fixed-point arithmetic:
- `SCALE_1M` (1,000,000) for mass, energy, and consumption.
- `SCALE_10K` (basis points) for growth rates, hunting probabilities, and mutation chances.

### 2. Level of Detail (LOD)
- **LOD 0**: Individual processing for complex animals.
- **LOD 1**: Statistical sampling and swarm aggregation for high-density species (plants, insects).

### 3. Concurrency Model
- **Cell-Level Locking**: Services lock only the cells they are working on.
- **Lock Ordering**: To prevent deadlocks during cross-cell movement, cells are always locked in (X, Y) order.
- **Tick-Level Caching**: Expensive global lookups (like Protection Maps) are cached once per tick.

### 4. Boilerplate-Free Domain
- Mandatory use of **Lombok** (`@Getter`, `@Setter`, `@Builder`) to keep domain logic clean.
- Removal of redundant Javadocs and FQNs; architectural "know-how" is deferred to `README.md`.
