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
Hunting logic is encapsulated in `HuntingStrategy`, allowing different behaviors for predators (e.g., pack hunting vs. solo hunting) without modifying the `FeedingService`.

**UML Pseudo-graphics:**
```text
+------------------+          +--------------------------------+
|  FeedingService  |--------->|        HuntingStrategy         |
+------------------+          +--------------------------------+
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
To allow rendering or state analysis without locking the active simulation, the engine supports creating "Snapshots".

**UML Pseudo-graphics:**
```text
+-----------------------+          +-----------------------+
|    SimulationWorld    |--------->|     WorldSnapshot     |
+-----------------------+          +-----------------------+
| + createSnapshot()    |          | - nodeSnapshots: List |
+-----------------------+          +-----------------------+
```

---

## 3. Data Flow & Execution Model

1. **Initialization**: `SimulationEngine` builds the `SimulationContext` using a `SimulationPlugin`.
2. **Tick Cycle**:
   - `GameLoop.runTick()` increments the tick counter.
   - `World.tick()` updates global state (seasons, protection maps).
   - Tasks are grouped by `Phase` (PREPARE, SIMULATION, POSTPROCESS).
   - Within each phase, tasks are sorted by `priority`.
   - `CellService` tasks are executed in parallel across `WorkUnits` (chunks).
3. **Locking Strategy**: `Cell` level `ReentrantLock` ensures thread-safety during parallel entity manipulation (e.g., movement between cells).

---

## 4. Configuration System

The `Configuration` class uses reflection to load parameters from `species.properties` or System properties.
- Prefix: `island.`
- Example: `island.islandWidth=10`
- Supports `int` and `long` fields automatically.

---

## 5. Domain Specific Patterns (Nature)
- **AnimalFactory**: Uses the Factory pattern to create organisms based on `SpeciesKey`.
- **SpeciesRegistry**: A centralized, non-static registry of all available species, their metadata (`AnimalType`), and unique `SpeciesKey` instances.
- **ComponentRegistry**: An instance-based registry that maps ECS component classes to stable integer indices. This enables high-performance array-based storage in `ArrayComponentStore` while maintaining isolation between concurrent simulation instances.
- **NatureDomainContext**: Uses the **Builder** pattern to aggregate all domain services (statistics, registry, etc.) for easier dependency injection into the `Island`.
