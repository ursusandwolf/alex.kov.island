# Island Simulator Architecture (v1.5)

## Class Diagram Concepts

### Engine Layer
- `SimulationWorld` (Island): Central hub. Manages spatial entities and notifies about lifecycle events.
- `SimulationNode` (Cell): Spatial unit. Uses fine-grained thread safety.
- `GameLoop`: Orchestrates the simulation lifecycle. Uses constructor injection for `ExecutorService` and `PhaseScheduler`.
- `PhaseScheduler`: Groups and sorts tasks by `Phase` and priority. Injected into `GameLoop`.
- `ParallelDispatcher`: Manages `CellProcessor` pool. Injected into `PhaseScheduler`.
- `CellService`: Interface for parallel business logic (Feeding, Movement, etc.).
- `SimulationMetrics`: Thread-safe builder for incremental aggregation of population and energy stats.

### Plugin System
- `SimulationPlugin`: Defines domain-specific world creation, task registration, and stop conditions.
- `SimulationEngine`: Orchestrates the assembly and lifecycle of the simulation.

### Domain Layer (Lombok Powered)
- `Organism`: Base for all life. Standardized on `long` energy (fixed-point).
- `Animal` (Herbivore/Predator): LOD 0 entities with individual logic and components.
- `SwarmOrganism`: LOD 1 entities (Plants, Butterflies) using mass-based aggregation.
- `EntityContainer`: O(1) management using indexed buckets and `LinkedHashSet`.

### Services (The Logic)
- `MovementService`: Coordinate-ordered cell transitions.
- `FeedingService`: Optimized hunting/grazing with pre-calculated interaction matrices.
- `ReproductionService`: Population growth with LOD scaling.
- `LifecycleService`: Aging and metabolism.
- `CleanupService`: O(1) removal and pool-based recycling of dead entities.
- `StatisticsService`: Zero-scan reporting using pre-aggregated metrics via `EventBus`.

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
    +-------+-------+              |     CellService<T,N>    |
    |               |              +-------------------------+
+---+----+      +---+----+         | + processCell(node, t)  |
| Island |      | CityMap|         +-------------------------+
+--------+      +--------+
```

### 5. Boilerplate-Free Domain
- Mandatory use of **Lombok** (`@Getter`, `@Setter`, `@Builder`) to keep domain logic clean.
