# Island Ecosystem Simulator: Technical Documentation

## 1. Architectural Overview

The project is a modular simulation engine designed to support multiple "domains" or "worlds" through a plugin system. The core engine is domain-agnostic, handling task scheduling, parallel execution, and lifecycle management, while specific logic (like nature, animals, or city building) is implemented in plugins.

### High-Level Components
- **Engine**: The core infrastructure (`com.island.engine`).
- **Nature Domain**: The primary simulation implementation (`com.island.nature`).
- **SimCity Domain**: A secondary simulation implementation for testing architectural flexibility (`com.island.simcity`).
- **Utilities**: Shared helper classes (`com.island.util`).

---

## 2. Design Patterns

### 2.1 Plugin Pattern
The engine uses the Plugin pattern to decouple the simulation loop from domain-specific logic.
**UML Pseudo-graphics:**
```text
+------------------+          +--------------------------------------------+
| SimulationEngine |          |             SimulationPlugin<T>            |
+------------------+          +--------------------------------------------+
| + build()        |<>------->| + createWorld(EventBus): SimulationWorld    |
+------------------+          | + registerTasks(GameLoop, World, EventBus) |
                              +--------------------------------------------+
                                               ^
                                               |
```

                        +----------------------+----------------------+
                        |                                             |
            +-----------------------+                 +-----------------------+
            |      NaturePlugin     |                 |      SimCityPlugin    |
            +-----------------------+                 +-----------------------+
            | - domainContext       |                 | - cityMap             |
            +-----------------------+                 +-----------------------+
```

### 2.2 Task Scheduling & Phase Management
The `GameLoop` delegates task orchestration to `PhaseScheduler` and parallel execution to `ParallelDispatcher`. Tasks are organized by `Phase` and `priority`.

**UML Pseudo-graphics:**
```text
+------------------+       +------------------+       +---------------------+
|     GameLoop     |------>|  PhaseScheduler  |------>|  ParallelDispatcher |
+------------------+       +------------------+       +---------------------+
| + runTick()      |       | + schedule()     |       | + dispatch()        |
+------------------+       +------------------+       +---------------------+
                                     |                           |
                                     v                           |
                           +---------------------------------+   |
                           |          ScheduledTask          |   |
                           +---------------------------------+   |
                           | + phase(): Phase                |   |
                           | + priority(): int               |   |
                           | + executionMode(): ExecMode     |   |
                           | + asParallelTask(): ParallelTask|   |
                           +---------------------------------+   |
                                     ^                           |
                                     |                           |
                           +---------------------------------+   |
                           |       ParallelTask<T>           |<--+
                           +---------------------------------+
                           | + beforeTick(tick)              |
                           | + processCell(node, tick)       |
                           | + afterTick(tick)               |
                           +---------------------------------+
                                     ^
                                     |
                           +---------------------------------+
                           |         CellService<T>          |
                           +---------------------------------+
```

### 2.3 Observer Pattern (World Events)
The `SimulationWorld` publishes events to the `EventBus` when entities are added or removed. This decouples the spatial grid from statistics or external services.

**UML Pseudo-graphics:**
```text
+--------------------------+          +-----------------------+
|   SimulationWorld<T>     |          |       EventBus        |
+--------------------------+          +-----------------------+
| + onEntityAdded(T)       |--------->| + publish(Object)     |
| + onEntityRemoved(T)     |          | + subscribe(Class, S) |
+--------------------------+          +-----------------------+
             ^                                    
             |                                    
      +------+-------+                    
      |    Island    |
      +--------------+
```

### 2.4 Strategy Pattern (Hunting)
Hunting logic is encapsulated in `HuntingStrategy`, allowing different behaviors for predators (e.g., pack hunting vs. solo hunting) without modifying the `AnimalFeedingSystem`.

**UML Pseudo-graphics:**
```text
+---------------------+          +--------------------------------+
| AnimalFeedingSystem |--------->|        HuntingStrategy         |
+---------------------+          +--------------------------------+
                              | + selectPrey(pred, provider)   |
                              | + selectPackPrey(pack, prov)   |
                              +--------------------------------+
                                              ^
                                              |
                                   +----------+----------+
                                   | DefaultHuntingStrat |
                                   +---------------------+
```

### 2.5 Snapshot Pattern
To allow rendering or state analysis without locking the active simulation, the engine supports creating "Snapshots". Headless mode skips snapshot creation entirely to optimize performance.

**UML Pseudo-graphics:**
```text
+-----------------------+          +-----------------------+
|    SimulationWorld    |--------->|     WorldSnapshot     |
+-----------------------+          +-----------------------+
| + createSnapshot()    |          | - nodeSnapshots: List |
+-----------------------+          +-----------------------+
                                              ^
                                              |
                                  +-----------+-----------+
                                  |      HeadlessView     | (No-op)
                                  +-----------------------+
```

### 2.6 System Execution Graph (ECS)
The engine automatically resolves dependencies between `EntitySystem` instances by analyzing their `readComponents` and `writeComponents` sets. Systems with non-overlapping write sets (and no read/write conflicts) are grouped into parallel batches.

**UML Pseudo-graphics:**
```text
+-----------------------+          +------------------------+
|   SystemExecutionGraph|--------->|    ParallelDispatcher  |
+-----------------------+          +------------------------+
| + buildSchedule(tasks)|          | + dispatch(batch)      |
+-----------------------+          +------------------------+
```

### 2.7 GC & Allocation Optimization
To support high-frequency ticks in large-scale simulations, the engine employs several object reuse strategies:
- **Schedule Caching**: `PhaseScheduler` caches the execution graph, avoiding re-calculating dependency batches if the task list hasn't changed.
- **Set-Free Conflict Detection**: `SystemExecutionGraph` utilizes list-based intersection checks to avoid object churn from temporary `HashSet` creations in the hot path.
- **Collection Pooling**: Core scheduler structures like `EnumMap` and `ArrayList` are retained as fields and cleared between ticks.

---

## 3. Data Flow & Execution Model

1. **Initialization**: `SimulationEngine` builds the `SimulationContext` using a `SimulationPlugin`.
2. **Tick Cycle**:
   - `GameLoop.runTick()` increments the tick counter.
   - `World.tick()` updates global state (seasons, protection maps).
   - Tasks are grouped by `Phase` (PREPARE, SIMULATION, POSTPROCESS).
   - Within each phase, tasks are sorted by `priority`.
   - `CellService` tasks are executed in parallel across `WorkUnits` (chunks).
3. **Locking Strategy**: 
   - **Cell-Level Locking**: `Cell` uses `ReentrantReadWriteLock`. 
   - **Deadlock Prevention**: All iteration methods (`forEachEntity`, `query`, etc.) follow a **"copy-under-read-lock, then execute"** pattern. This ensures the read lock is released before any action that might require a write lock (e.g., moving an entity) is executed, preventing read-to-write upgrade deadlocks.
   - **Multi-Locking**: `GridUtils.executeWithDoubleLock` uses a global coordinate-based ordering to prevent deadlocks during inter-cell movement.
4. **Configuration System**:

The `Configuration` class uses reflection to load parameters from `species.properties` or System properties.
- Prefix: `island.`
- Example: `island.islandWidth=10`
- Supports `int` and `long` fields automatically.

---

## 5. Domain Specific Patterns (Nature)
- **AnimalFactory**: Uses the Factory pattern to create organisms based on `SpeciesKey`.
- **SpeciesRegistry**: A centralized, non-static registry of all available species, their metadata (`AnimalType`), and unique `SpeciesKey` instances.
- **Climate System**: A global service that updates environmental factors (Season, Temperature) during the `PREPARE` phase. These factors are consumed by ECS systems to modify organism behavior.
- **ComponentRegistry**: An instance-based registry that maps ECS component classes to stable integer indices. This enables high-performance array-based storage in `ArrayComponentStore` while maintaining isolation between concurrent simulation instances.
- **NatureDomainContext**: Uses the **Builder** pattern to aggregate all domain services (statistics, registry, etc.) for easier dependency injection into the `Island`.
