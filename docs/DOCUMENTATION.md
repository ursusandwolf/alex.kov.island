# Island Ecosystem Simulator: Technical Documentation

## 1. Architectural Overview

The project is a modular simulation engine designed to support multiple "domains" or "worlds" through a plugin system. The core engine is domain-agnostic, handling task scheduling, parallel execution, and lifecycle management, while specific logic (like nature, animals, or city building) is implemented in plugins.

### High-Level Components
- **Engine**: The core infrastructure (`com.island.engine`).
- **Nature Domain**: The primary simulation implementation (`com.island.nature`), using a hybrid ECS approach for organisms.
- **SimCity Domain**: A secondary simulation implementation for testing architectural flexibility (`com.island.simcity`), using a pure ECS approach with generic entities and components.
- **Utilities**: Shared helper classes (`com.island.util`).

---

## 2. Design Patterns

### 2.1 Plugin Pattern
The engine uses the Plugin pattern to decouple the simulation loop from domain-specific logic.

### 2.2 Task Scheduling & Phase Management
The `GameLoop` delegates task orchestration to `PhaseScheduler` and parallel execution to `ParallelDispatcher`. Tasks are organized by `Phase` and `priority`.

### 2.3 Observer Pattern (World Events)
The `SimulationWorld` publishes events to the `EventBus` when entities are added or removed. This decouples the spatial grid from statistics or external services.

### 2.4 Strategy Pattern (Hunting)
Hunting logic is encapsulated in `HuntingStrategy`, allowing different behaviors for predators (e.g., pack hunting vs. solo hunting) without modifying the `AnimalFeedingSystem`.

### 2.5 Snapshot Pattern
To allow rendering or state analysis without locking the active simulation, the engine supports creating "Snapshots". Headless mode skips snapshot creation entirely to optimize performance.

### 2.6 System Execution Graph (ECS)
The engine automatically resolves dependencies between `EntitySystem` instances by analyzing their `readComponents` and `writeComponents` sets. Systems with non-overlapping write sets (and no read/write conflicts) are grouped into parallel batches. This is used extensively in both Nature and SimCity domains.

### 2.7 SimCity Pure ECS
Unlike the Nature domain which uses specialized `Animal`/`Biomass` classes, the SimCity domain uses a pure ECS approach:
- **Generic Entity**: `SimEntity` is a generic container for components.
- **Components**: Data is stored in `PopulationComponent`, `BuildingComponent`, and `EconomyComponent`.
- **Stateless Systems**: Logic is implemented in `PopulationService`, `EconomyService`, and `CityAnalyticsService` which operate strictly on components.

### 2.8 GC & Allocation Optimization
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
- **Landscape & Rivers**: The `WorldInitializer` implements procedural terrain generation. A winding river of `WATER` terrain acts as a natural barrier. Movement accessibility is determined by the intersection of terrain properties and animal capabilities (swimming, flying, or high speed for jumping).
- **ComponentRegistry**: An instance-based registry that maps ECS component classes to stable integer indices. This enables high-performance array-based storage in `ArrayComponentStore` while maintaining isolation between concurrent simulation instances.
- **NatureDomainContext**: Uses the **Builder** pattern to aggregate all domain services (statistics, registry, etc.) for easier dependency injection into the `Island`.

---

## 6. Architecture Evolution Plan (Completed)

The project has fully transitioned from a monolith to a library-ready, multi-module architecture.

### 6.1 Final Module Structure
1. **island-engine**: Pure simulation infrastructure. Zero knowledge of "animals" or "cities".
   - Defines `SimulationWorld<T>`, `SimulationNode<T>`, `Entity`, `Component`.
   - Orchestrates execution via `GameLoop` and `PhaseScheduler`.
   - Exports stable API via `module-info.java`.
2. **island-nature**: Island ecosystem plugin.
   - Implements `Organism` (Animal/Biomass) hierarchy.
   - Provides specialized ECS systems for hunger, movement, and growth.
3. **island-simcity**: Urban simulation plugin.
   - Demonstrates "Pure ECS" usage without class inheritance.
   - Uses `SimEntity` with `PopulationComponent`, `BuildingComponent`.
4. **island-app**: Launchers and integration tests.
   - Orchestrates plugin assembly.
   - Hosts `ArchitectureTest` for project-wide constraint enforcement.

### 6.2 API Contracts & Stability
- **@EngineAPI**: Classes marked with this are stable and intended for plugin developers. Includes `SimulationEngine`, `GameLoop`, `EventBus`, `SimulationWorld`, `Entity`, and core ECS interfaces.
- **@InternalEngine**: Classes marked with this are implementation details and may change. Plugins are strictly forbidden from using these (enforced by ArchUnit and JPMS). Includes `ParallelDispatcher`, `PhaseScheduler`, and internal ECS storage implementations.
- **JPMS (Java Module System)**: The engine module strictly controls its exports. Specifically, `com.island.engine.parallel` is NOT exported to any other module, ensuring that internal threading and dispatching logic remains encapsulated.
- **Architecture Enforcement**: `ArchitectureTest` in `island-app` uses ArchUnit to verify that no classes in `nature` or `simcity` packages depend on classes marked as `@InternalEngine`.
