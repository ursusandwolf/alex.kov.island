# Island Simulator Architecture (v1.8.1)

## Class Diagram Concepts

### Engine Layer
- `SimulationWorld` (Island): Central hub. Manages spatial entities and acts as the **single source of truth** for lifecycle events.
- `SimulationNode` (Cell): Spatial unit. Uses fine-grained thread safety.
- `GameLoop`: Orchestrates the simulation lifecycle.
- `PhaseScheduler`: Groups and sorts tasks by `Phase` and priority. Uses `SystemExecutionGraph` for Batching.
- `SystemExecutionGraph`: Optimized static dependency resolver with garbage-free conflict detection.
- `ParallelDispatcher`: Manages `CellProcessor` pool for parallel execution.
- **API Markers**: `@EngineAPI` (Public Contract) and `@InternalEngine` (Implementation detail).

### Domain Layer (Lombok Powered)
- `SpeciesRegistry`: Centralized, non-static registry for species metadata.
- `ClimateService`: Global system for managing environmental state.
- `Organism`: Base for all life in Nature domain. Optimized with volatile ECS components.
- `SimEntity`: Pure ECS entity for SimCity domain. A generic container.
- **ECS Components**:
    - `ConsumableComponent`: Typed resource consumption with `ConsumeAction<Cell>`.
    - `PopulationComponent`, `BuildingComponent`, `EconomyComponent`: SimCity state containers.

### Services (The Logic)
- **Specialized ECS Systems**:
    - `AnimalHealthSystem` / `BiomassGrowthSystem`: Handle lifecycle logic (metabolism, growth, aging) influenced by Climate factors.
    - `AnimalMovementSystem` / `BiomassMovementSystem`: Coordinate spatial transitions using `MovementComponent` data.
    - `AnimalFeedingSystem`: Optimized hunting/grazing with type-safe `ConsumableComponent<T>` and `ConsumeAction<T>`.
    - `AnimalReproductionSystem`: Population growth with LOD scaling and `ReproductionComponent` support.
    - `PopulationService` / `EconomyService`: SimCity systems operating on `PopulationComponent`, `BuildingComponent`, and `EconomyComponent`.
    - `ConnectivityService`: Manages multiple independent networks (Road, Water, Rail, Metro).
    - `PollutionService`: Simulates air and water pollution generation, diffusion, and dissipation.

- `CleanupService`: O(1) removal and pool-based recycling of dead entities.
- `StatisticsService`: Zero-scan reporting using pre-aggregated metrics via `EventBus` with specialized `AnimalBornEvent` and `AnimalDiedEvent` support.

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
- **Locking Pattern**: Uses **"Copy-under-Read-Lock, then Execute"** to prevent read-to-write upgrade deadlocks during iteration.
- **Lock Ordering**: To prevent deadlocks, cells are always locked in (X, Y) order.
- **Thread-Safe Components**: Organism components use `volatile` fields and `ConcurrentHashMap`.
- **Robust EventBus**: Iterative type resolution and subscriber exception isolation.

## 4. SimCity 4 Mechanics (SC4 Roadmap)
The SimCity plugin follows a structured roadmap (`docs/simcity_todo.md`) to implement advanced mechanics:
- **Phase 1 (Environmental)**: Pollution and Desirability [COMPLETED].
- **Phase 2 (Density & Wealth)**: Residential/Commercial/Industrial wealth tiers and density levels.
- **Phase 3 (Services)**: Education (EQ) and Healthcare.

## 5. Visual Architecture (Pseudo-UML)

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
                                                ^
                                                |
                                   +------------+------------+
                                   |      SimulationView     |
                                   +-------------------------+
                                   | + display(snapshot)     |
                                   +------------^------------+
                                                |
                                     +----------+----------+
                                     |                     |
                            +--------+-------+    +--------+-------+
                            |   ConsoleView  |    |  HeadlessView  |
                            +----------------+    +----------------+
```

### 5. Boilerplate-Free Domain
- Mandatory use of **Lombok** (`@Getter`, `@Setter`, `@Builder`) to keep domain logic clean.
