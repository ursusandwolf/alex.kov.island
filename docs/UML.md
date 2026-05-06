# Island Simulator Architecture (v1.6)

## Class Diagram Concepts

### Engine Layer
- `SimulationWorld` (Island): Central hub. Manages spatial entities and acts as the **single source of truth** for lifecycle events (`onEntityAdded`, `onEntityRemoved`).
- `SimulationNode` (Cell): Spatial unit. Uses fine-grained thread safety and template methods for lifecycle hooks.
- `GameLoop`: Orchestrates the simulation lifecycle. Uses constructor injection for `ExecutorService` and `PhaseScheduler`.
- `PhaseScheduler`: Groups and sorts tasks by `Phase` and priority. Uses an abstract `ParallelTask` visitor-like dispatching to avoid domain leaks.
- `ParallelDispatcher`: Manages `CellProcessor` pool for parallel execution of `ParallelTask` groups.
- `ParallelTask`: Abstract interface for per-cell parallel execution.
- `CellService`: Simplified domain interface for simulation logic.

### Domain Layer (Lombok Powered)
- `SpeciesRegistry`: Centralized, non-static registry for species metadata and unique `SpeciesKey` interning.
- `Organism`: Base for all life. Standardized on `long` energy (fixed-point). Features thread-safe `energyLock` and volatile ECS components.
- `Animal` (Herbivore/Predator): LOD 0 entities with individual logic and components.
- `SwarmOrganism`: LOD 1 entities (Plants, Butterflies) using mass-based aggregation.
- `EntityContainer`: O(1) management using indexed buckets and `LinkedHashSet`.

### Services (The Logic)
- `MovementService`: Coordinate-ordered cell transitions. Overrides `DeathCause` if exhausted.
- `FeedingService`: Optimized hunting/grazing with pre-calculated interaction matrices. Supports pack hunting.
- `ReproductionService`: Population growth with LOD scaling.
- `LifecycleService`: Aging and metabolism with seasonal and hibernation modifiers.
- `CleanupService`: O(1) removal and pool-based recycling of dead entities.
- `StatisticsService`: Zero-scan reporting using pre-aggregated metrics via `EventBus` with hierarchical type support.

## Core Principles

### 1. Integer-Based Arithmetic
Deterministic results using fixed-point arithmetic:
- `SCALE_1M` (1,000,000) for mass, energy, and consumption.
- `SCALE_10K` (basis points) for growth rates, hunting probabilities, etc.

### 2. Level of Detail (LOD)
- **LOD 0**: Individual processing for complex animals.
- **LOD 1**: Swarm aggregation for high-density species.

### 3. Concurrency Model
- **Grouped Parallelism**: Multiple `CellService` tasks are processed per cell in a single parallel pass.
- **Lock Ordering**: To prevent deadlocks, cells are always locked in (X, Y) order.
- **Thread-Safe Components**: Organism components use `volatile` fields and `ConcurrentHashMap`.
- **Robust EventBus**: Iterative type resolution and subscriber exception isolation.

## 4. Visual Architecture (Pseudo-UML)

```text
+-----------------------+          +-------------------------+
|   SimulationEngine    |          |   SimulationPlugin<T>   |
+-----------------------+          +-------------------------+
| + build()             |          | + createWorld(Bus)      |
| + stop()              |<>--------| + registerTasks(...)    |
| - eventBus: EventBus  |          | + shouldStop()          |
+-----------+-----------+          +------------+------------+
            |                                   |
            |            (Builds)               |
            +-----------------------------------+
            |
            v
+-----------------------+          +-------------------------+
|     GameLoop<T>       |          |    PhaseScheduler<T>    |
+-----------------------+          +-------------------------+
| + runTick()           |<>------->| + execute(...)          |
| + start() / stop()    |          |          |              |
+-----------+-----------+          +----------+--------------+
            |                                 |
            |                                 v
+-----------+-----------+          +-------------------------+
|   SimulationWorld<T>  |          |  ParallelDispatcher<T>  |
+-----------------------+          +-------------------------+
| - eventBus: EventBus  |<---------| + dispatch(...)         |
| + onEntityAdded(T)    |          |<>--------|              |
| + onEntityRemoved(T)  |          +----------+--------------+
+-----------+-----------+                     |
            ^                                 v
            |                      +-------------------------+
    +-------+-------+              |     ParallelTask<T>     |
    |               |              +-------------------------+
+---+----+      +---+----+         | + processCell(node, t)  |
| Island |      | CityMap|         +------------^------------+
+--------+      +--------+                      |
                                   +------------+------------+
                                   |      CellService<T>     |
                                   +-------------------------+
```

### 5. Boilerplate-Free Domain
- Mandatory use of **Lombok** (`@Getter`, `@Setter`, `@Builder`) to keep domain logic clean.
