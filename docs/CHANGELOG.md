# Changelog

## [1.19.0] - 2026-05-06
### Added
- **ParallelTask Abstraction**: Introduced `ParallelTask` interface to decouple `PhaseScheduler` and `ParallelDispatcher` from domain-specific `CellService`.
- **Abstract Task Dispatching**: Implemented `asParallelTask()` in `ScheduledTask`, enabling the scheduler to identify and group parallelizable tasks without `instanceof` checks.

### Changed
- **API Simplification**: Removed the `N` generic parameter from `CellService`, resolving critical type erasure clashes and improving compatibility with the abstract `ParallelTask` hierarchy.
- **SpeciesRegistry Optimization**: Modernized `SpeciesRegistry` to use `Map.copyOf` for better immutability and cached `allSpeciesCodes` to eliminate redundant allocations in statistics collection.
- **Thread-Safety Documentation**: Explicitly marked `ParallelDispatcher` as `@NotThreadSafe` for concurrent `dispatch` calls to avoid synchronization overhead while maintaining single-threaded management safety.

### Fixed
- **Lombok Processing Conflicts**: Resolved a build-breaking erasure clash in `CellService` that was preventing proper annotation processing across the codebase.

## [1.18.0] - 2026-05-06
### Fixed
- **SpeciesKey Singleton Debt**: Refactored `SpeciesKey` to a pure value object and moved the interning registry to `SpeciesRegistry`. This eliminates global state and allows for multiple isolated simulation instances.
- **PhaseScheduler Race Condition**: Moved internal task grouping and parallel group lists to local variables within the `execute` method, ensuring thread-safety for concurrent scheduler usage.
- **ParallelDispatcher Leak**: Implemented pool shrinking logic in `ParallelDispatcher` to reclaim memory if the number of simulation work units decreases.

### Changed
- **Modern Java Refactoring**: Converted `SimulationContext` from a class to a Java `record`, reducing boilerplate and aligning with current Java standards.
- **API Cleanup**: Updated `SpeciesRegistry` and `InteractionMatrix` to provide centralized access to species keys, removing dependencies on static `SpeciesKey` factory methods.

## [1.17.0] - 2026-05-06
### Fixed
- **Double Death Reporting**: Fixed redundant `EntityDiedEvent` publications in `FeedingService`, `MovementService`, and `ReproductionService`. All death events are now published from a single source of truth: `Island.onEntityRemoved`.
- **GameLoop Concurrency**: Refactored `PhaseScheduler` to use local variables for task grouping, eliminating potential race conditions in parallel execution.
- **Test Verification**: Updated `GameLoopOptimizationTest` to explicitly verify `CellProcessor` reuse in the dispatcher pool via reflection.

### Added
- **EventBus Documentation**: Added Javadoc for `EventBus.subscribe()` explaining type hierarchy support and `Object.class` wildcard subscription.
- **Improved Metrics**: Stats now accurately reflect death causes without duplicates, ensuring reliable balance calibration.

## [1.16.0] - 2026-05-05
### Added
- **Lombok Integration**: Applied `@Slf4j`, `@Getter`, `@Setter`, and `@RequiredArgsConstructor` to `GameLoop`, `ParallelDispatcher`, and `PhaseScheduler` to minimize boilerplate.
- **Dependency Injection**: Refactored `GameLoop` to use constructor injection for its dependencies (`ExecutorService`, `PhaseScheduler`), improving testability and adhering to DIP.
- **Synchronous Fallback**: Implemented fallback execution in `ParallelDispatcher` to handle `RejectedExecutionException` gracefully by running tasks in the current thread.

### Changed
- **High-Precision Timing**: Switched from `System.currentTimeMillis()` to `System.nanoTime()` in the simulation loop for more accurate tick duration management.
- **Engine Bootstrapping**: Updated `SimulationEngine` to orchestrate the creation of `ExecutorService` and schedulers before injecting them into `GameLoop`.

### Fixed
- **Generic Syntax Error**: Fixed a malformed generic declaration in `ParallelDispatcher`.
- **Test Suite Alignment**: Mass-updated all engine and domain tests to support the new `GameLoop` constructor.

## [1.15.0] - 2026-05-05
### Added
- `PhaseScheduler` and `ParallelDispatcher` to handle simulation task orchestration.
- `shouldStop` hook to `SimulationPlugin` for domain-specific termination logic.
- `onSimulationStopped` lifecycle hook to `SimulationPlugin`.
- `maxSimulationDurationMs` and `monitoringIntervalMs` to `Configuration`.

### Changed
- Refactored `GameLoop` to use `PhaseScheduler` and `ParallelDispatcher`, eliminating God Class code smell.
- Simplified `SimulationWorld` interface by removing unused generic configuration parameter and explicit listener management.
- Replaced `WorldListener` mechanism with direct world notification methods (`onEntityAdded`, `onEntityRemoved`).
- Unified `NatureLauncher` logs to English and removed abstraction leaks.
- Improved `DefaultEventBus` type resolution using an iterative approach.

### Fixed
- Potential resource leak by calling `onSimulationStopped` in `SimulationEngine.stop()`.
- Data race in `CellProcessor` by marking shared fields as `volatile`.
- Silently failing fallback in `CellService.tick()`.

## [1.14.0] - 2026-05-05
### Fixed
- **Thread Safety**: Made `HealthComponent` and `AgeComponent` fields `volatile` to ensure visibility across threads during parallel simulation ticks.
- **Organism State**: Added `volatile` to `lastDeathCause` in `Organism` to ensure consistent death reporting across threads.

### Optimized
- **GameLoop GC Efficiency**: Introduced `CellProcessor` pooling and switched to `CountDownLatch` for parallel task execution. This eliminates thousands of `ArrayList`, `Callable`, `Future`, and lambda allocations per tick, significantly reducing GC pressure during heavy simulations.
- **Task Scheduling**: Reused `parallelGroup` and phase-based task lists to further minimize object churn in the simulation hot path.

### Improved
- **AlertService Refactoring**: Replaced magic string `"STARVATION"` with `DeathCause.HUNGER.name()` for better maintainability and alignment with the domain model.

## [1.13.0] - 2026-05-05
### Added
- **Enhanced EventBus**:
    - Implemented hierarchical event matching in `DefaultEventBus`, allowing subscribers to listen for superclasses or interfaces of published events.
    - Added `unsubscribe` mechanism to the `EventBus` interface for dynamic lifecycle management.
- **AlertService**: Introduced a new service that monitors simulation events and logs significant occurrences (e.g., starvation deaths).
- **New Tests**: Added `EventBusTest` to verify hierarchical matching and unsubscription.

### Optimized
- **Organism Component Access**: Replaced `ConcurrentHashMap` lookups with direct field references for "hot" components (`HealthComponent`, `AgeComponent`), significantly reducing overhead in the main simulation loop.
- **GameLoop Allocations**: Refactored `GameLoop` to reuse phase-based collection structures (`EnumMap` and `ArrayLists`), eliminating thousands of object allocations per tick.
- **Sampling Strategy**: Optimized `SamplingUtils.forEachSampled` to use $O(1)$ indexed access for `RandomAccess` lists, avoiding the $O(N)$ skip overhead of iterators.

### Fixed
- Fixed a syntax error in `Organism.java` that caused build failures.
- Unified hunger-related death causes for more consistent reporting.

## [1.12.0] - 2026-05-05
### Fixed
- **Double Death Reporting**: Eliminated double accounting of deaths in `StatisticsService`. Services no longer publish `EntityDiedEvent` directly; it is now published exclusively by `Island.onEntityRemoved` via the `WorldListener` interface, ensuring a single source of truth.
- **ECS Thread Safety**: Replaced `HashMap` with `ConcurrentHashMap` for organism components, preventing race conditions during parallel simulation steps.
- **EventBus Robustness**: Added error isolation to `DefaultEventBus.publish()`. Exceptions in one subscriber no longer prevent other subscribers from receiving events.

### Changed
- **EventBus Injection**: Refactored `SimulationWorld` and `SimulationPlugin` to enforce `EventBus` immutability. The `EventBus` is now passed via constructors and `createWorld()` instead of a setter.
- **CityMap API**: Cleaned up `CityMap` by using field-level Lombok annotations and making `EventBus` final.

## [1.11.0] - 2026-05-04
### Added
- **Event-Driven Architecture (EDA)**:
    - Implemented a lightweight `EventBus` in the core engine for decoupled communication.
    - Refactored all Nature services (`FeedingService`, `MovementService`, `ReproductionService`, `LifecycleService`) to publish domain events (`EntityBornEvent`, `EntityDiedEvent`).
    - Added specialized death causes: `EATEN_BY_PACK` and `REPRODUCTION_EXHAUSTION`.
- **ECS (Entity-Component-System) Transition**:
    - Introduced `Component` architecture with initial implementations: `AgeComponent`, `HealthComponent`, `MovementComponent`.
    - Refactored `Organism` to use a hybrid component-based approach, improving modularity for future engine evolution.
- **New Test Cases**: Added `SurvivalCalibrationTest.java` to verify long-term ecosystem stability after parameter recalibration.

### Changed
- **Ecosystem Balance & Terminology**:
    - Renamed "Starvation" to "Hunger" across the codebase (including `isHungry()`, `hungryCount`, and UI labels) for better domain alignment.
    - Recalibrated `species.properties`: adjusted base reproduction chances, presence probabilities, and prey success rates to prevent sudden extinctions.
    - Reduced `REPRODUCTION_MIN` energy threshold in `EnergyPolicy` from 50% to 35% to facilitate population recovery.
- **UI/UX Improvements**:
    - Updated `ConsoleView` with detailed death statistics, aggregating causes into categories (Hunger, Old Age, Exhausted, Predation).
    - Improved satiety visualization with color-coded progress bars.

### Improved
- **Code Standards**: Refactored test files to remove Fully Qualified Names (FQNs) in code bodies, ensuring adherence to the project's coding standards for explicit imports and clean syntax.
- **Service Decoupling**: Enhanced `MovementService` and `ReproductionService` to utilize the `EventBus`, removing direct dependencies on statistics aggregation logic.
- **Configuration Management**: Refactored `SimulationConstants` and `Configuration` to use unified "Hunger" terminology and updated default values.

## [1.10.0] - 2026-05-04
### Added
- **Endangered Species Protection**: Introduced a set of parameters in `species.properties` (threshold, repro bonus, speed bonus, hide chance) to support adaptive survival mechanics for dwindling populations.

### Changed
- **Ecosystem Balancing & Calibration**:
    - Reduced movement cost (`speedMoveCostStepBP`) from 100 to 50 Basis Points to increase survival during migration.
    - Lowered reproduction energy threshold (`REPRODUCTION_MIN`) from 70% to 50% to encourage population growth.
    - Recalibrated hunting success probabilities in `species.properties`, generally reducing predator efficiency to prevent over-predation.
    - Increased base reproduction chances and maximum offspring counts across all `SizeClass` categories.
- **World Initialization**:
    - Enhanced `WorldInitializer` to ensure at least 2 individuals (a breeding pair) are spawned for species with `maxPerCell >= 2`.
    - Corrected initial biomass calculation to respect `presenceChance` and `maxPerCell` capacity.
- **Biomass & Life Cycles**:
    - Fixed `Butterfly` and `Caterpillar` lifecycle transitions by properly initializing their capacity and avoiding dummy object creation.
    - Improved `GenericBiomass` calculation to use entity weight instead of energy capacity for initial sizing.

### Improved
- **Metabolism Modeling**: Added robustness to `Organism.getDynamicMetabolismRate()` with null checks for `HealthComponent`.
- **Test Infrastructure**:
    - Enabled `StressStabilityTest` (previously disabled) and tuned it for 200-tick verification on a 5x5 grid.
    - Increased `ExtinctionBalanceTest` iterations from 3 to 10 to improve reliability of extinction detection.
    - Updated `SimulationOptimizationTest` to align with the new `SwarmOrganism` constructor requirements.

## [1.9.0] - 2026-05-03
### Added
- **ExecutionMode Enum**: Introduced `ExecutionMode` (`SEQUENTIAL`, `PARALLEL`) to the core engine, allowing tasks to explicitly declare their thread-safety and execution preference.
- **Extended Simulation Phases**: Added `PREPARE` and `POSTPROCESS` phases to `Phase.java` for finer control over the simulation lifecycle.

### Changed
- **Engine Decoupling**: 
    - Removed `SimulationRenderer` from `SimulationContext` and `SimulationEngine.start()`, fully isolating the core engine from the UI/visual layer.
    - Refactored `GameLoop` to use `ExecutionMode` instead of domain-specific `instanceof CellService` checks for parallel execution.
- **Concurrency & Safety**:
    - **Atomic Movement**: Improved `moveEntity` in `CityMap` and `Island` with rollback logic to ensure entities are never lost or duplicated during inter-node movement.
    - **Encapsulation**: Secured `CityTile` by removing direct access to the internal entities list, enforcing the use of thread-safe iteration and modification methods.
    - **Visibility**: Marked `cachedChunks` in `CityMap` as `volatile` to guarantee safe publication across threads during initialization.

### Improved
- **Scheduler Efficiency**: Optimized `GameLoop` to reduce allocations in the "hot" tick path by reusing phased task structures and improving error handling in parallel worker groups.
- **Test Suite Alignment**: Updated `GameLoopConcurrencyTest`, `ExtinctionBalanceTest`, and `StressStabilityTest` to reflect the refactored engine API and verify new execution modes.

## [1.8.0] - 2026-05-03
### Added
- **Detailed Documentation**: Created `docs/DOCUMENTATION.md` providing a comprehensive overview of the system architecture, including UML pseudo-graphics for core design patterns (Plugin, Task Scheduling, Observer, Strategy, etc.).

### Improved
- **Architectural Abstraction**: Refactored `AbstractService` and its specialized subclasses (`FeedingService`, `MovementService`, etc.) to use the `SimulationNode<Organism>` interface instead of the concrete `Cell` class in the `processCell` signature. This improves domain isolation and testability.
- **Configuration Management**: Eliminated the magic number for season duration in `Island.java` by moving it to the `Configuration` system.
- **Code Integrity**: Removed redundant `instanceof` checks and dead code branches in the `AbstractService` hierarchy.

## [1.7.0] - 2026-05-02
### Added
- **GameLoop Thread Selection**: Enhanced `GameLoop` to respect the `threadCount` parameter. It now dynamically selects between a fixed thread pool (for controlled concurrency) and Java 21 Virtual Threads (for maximum throughput) based on configuration.

### Improved
- **GC Performance & Allocation Optimization**: 
    - Refactored `Cell` and `EntityContainer` to support zero-allocation iteration over entities using the internal `forEach` pattern.
    - Eliminated redundant intermediate list creations in "hot" simulation paths, significantly reducing GC pressure.
- **Concurrency & State Management**:
    - Cleaned up `Organism` state by removing redundant `volatile` markers on fields already protected by explicit locks, improving cache locality and performance.
    - Consolidated simulation constants into the `Configuration` system, eliminating data duplication and ensuring a single source of truth for multi-instance simulations.
- **Architectural Consistency**: Verified that `SimCity` services correctly implement the `CellService` interface, ensuring they benefit from the engine's parallel execution and priority scheduling.

## [1.6.0] - 2026-05-02
### Added
- **Context Injection**: Introduced `NatureDomainContext` to encapsulate domain-specific components (registries, factories, services). This enables better testability and multi-instance support by using constructor injection throughout the `nature` domain.
- **Structured Logging**: Integrated SLF4J and Logback. Replaced all `System.out` and `System.err` calls in `NatureLauncher`, `SimulationEngine`, and `GameLoop` with SLF4J Loggers (via Lombok's `@Slf4j`).
- **Statistics Service Tests**: Added `StatisticsServiceTest.java` to verify population aggregation and ensure no double counting of entities.

### Changed
- **Enhanced Monitoring**: Refactored `NatureLauncher` to use `ScheduledExecutorService` for non-blocking monitoring of extinction and duration, replacing blocking `Thread.sleep` calls.
- **Statistics Service Refinement**: Refactored `StatisticsService` by splitting large methods and improving data aggregation logic. derive `getTotalPopulation` from merged species counts to ensure consistency.
- **Biomass Logic Optimization**: Extracted biomass management and movement logic from `Island.java` into a dedicated `DefaultBiomassManager` class, adhering to SRP.

### Improved
- **Javadoc Audit**: Completed a documentation audit for all new specialized interfaces and core components (`NatureStatistics`, `NatureDomainContext`, etc.).
- **Build Configuration**: Updated `pom.xml` with dependencies for SLF4J API and Logback Classic.

### Fixed
- **Double Counting Fix**: Resolved a potential bug in `StatisticsService` where the total population could be inconsistent with individual species counts by deriving total from merged counts.
- **Test Stability**: Fixed multiple compilation errors and redundant variables in the test suite caused by architectural changes.
