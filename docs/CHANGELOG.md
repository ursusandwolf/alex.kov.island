# Changelog

## [1.38.0] - 2026-05-10
### Fixed
- **SoA Thread-Safety**: Fixed critical race conditions in `HealthSoAStore` and `AgeSoAStore` by making array fields `volatile` and synchronizing the `ensureCapacity` method.
- **Entity ID Management**: Refactored `EntityIdManager` to use `ConcurrentLinkedQueue` for ID recycling, eliminating mixed synchronization and improving efficiency.

### Changed
- **Module Encapsulation**: Refined JPMS exports in `island-nature` and `island-simcity`. Internal packages (`model`, `service`, `entities.components`) are no longer exported, enforcing strict implementation hiding.
- **Performance Optimization**: Optimized `AnimalHealthSystem` to access `HealthStorage` and `AgeStorage` directly by ID, reducing the overhead of object-oriented wrappers during energy consumption and age tracking.

### Added
- **Build Infrastructure**: 
    - Integrated `maven-enforcer-plugin` to guarantee build consistency.
    - Added `jacoco-maven-plugin` for code coverage reporting (60% threshold).
    - Added `maven-source-plugin` and `maven-javadoc-plugin` for release artifact generation.
    - Integrated `PITest` for mutation testing and `JMH` for performance benchmarking in the engine module.
- **Plugin Discovery**: Added `provides`/`uses` directives to `module-info.java` to support standard Java `ServiceLoader` for simulation plugins.
- **Documentation**: Enhanced Javadocs for `@EngineAPI` classes (`SimulationEngine`, `SimulationPlugin`, `SimulationContext`) with usage examples and clear contracts.

## [1.37.0] - 2026-05-09
### Fixed
- **Movement Deadlock**: Fixed a critical deadlock in `Island.moveOrganism` caused by nested acquisitions of non-reentrant `StampedLock` write locks. Introduced lock-free internal methods (`canAcceptInternal`, `addAnimalInternal`, `removeAnimalInternal`) in `Cell` for safe execution within `GridUtils.executeWithDoubleLock`.

### Changed
- **Structure of Arrays (SoA) Optimization**:
    - Fully migrated `Organism` component state (`HealthComponent`, `AgeComponent`) to high-density SoA storage (`HealthSoAStore`, `AgeSoAStore`).
    - Stripped heap-allocated state from `HealthComponent` and `AgeComponent`, reducing them to empty marker classes. This maintains ECS query compatibility and `SystemExecutionGraph` parallel conflict detection while drastically reducing GC pressure for millions of entities.
    - Added primitive fallback fields directly in `Organism` to support isolated tests and unbound initialization.

## [1.36.0] - 2026-05-09
### Fixed
- **JPMS & API Isolation**: Resolved critical architectural violations where internal engine packages were exported and used by the nature module. 
    - Introduced `@EngineAPI` interfaces: `EntityIdProvider`, `HealthStorage`, and `AgeStorage` in `com.island.engine.core`.
    - Removed `exports com.island.engine.internal` from `island-engine/module-info.java`.
    - Refactored `NatureDomainContext` and `NatureDomainContextFactory` to use the new public storage interfaces instead of internal engine classes, satisfying ArchUnit rules in `ArchitectureTest`.
- **SimulationConfig Logic**: Fixed inverted logic in `SimulationConfig.defaultFor(threadCount)` where `threadCount > 0` incorrectly resulted in `SEQUENTIAL` execution mode.
- **EconomySystem Stability**: Replaced the `UnsupportedOperationException` in the placeholder `EconomySystem.process()` with a TODO comment to prevent crashes during SimCity execution.

### Added
- **Engine Convenience API**: Re-introduced a convenient 3-argument `build` overload in `SimulationEngine` to support existing tests while maintaining compatibility with the new `SimulationConfig` model.
- **Continuous Integration**: Added `.github/workflows/ci.yml` to automate Maven builds and ArchUnit architecture verification on every push and pull request.

## [1.35.0] - 2026-05-09
### Changed
- **Engine Quality & Performance**:
    - **Cell & EntityContainer**: Migrated `Cell` from `ReentrantReadWriteLock` to `StampedLock` with optimistic reads to significantly reduce lock contention overhead. Eliminated O(N) iteration overheads in `EntityContainer` by replacing some inner collections with pre-calculated counters and `LinkedHashMap` indexing.
    - **GridUtils**: Replaced explicit coordinate-based locking with `System.identityHashCode` ordering and a `tryLock` fallback to prevent deadlocks without spatial coupling.
    - **SystemExecutionGraph**: Optimized conflict detection by avoiding `HashSet` allocations and using direct list iteration. This reduces garbage collection pressure during task scheduling.
    - **DefaultEventBus**: Optimized type hierarchy traversal by replacing `LinkedList` with `ArrayDeque` for improved queue performance.
    - **Verification**: Verified module-path compilation and packaging for all engine and domain modules.

## [1.34.0] - 2026-05-09
### Changed
- **Engine Quality & Performance**:
    - **GameLoop Robustness**: Refactored `GameLoop` with `AtomicBoolean` for robust thread-safe `start/stop` operations. Fixed potential deadlocks during thread joining.
    - **Scheduling Optimization**: Updated `PhaseScheduler` to use `tasksVersion` counter for change detection, eliminating expensive $O(N)$ hash calculations every tick.
    - **SystemExecutionGraph**: Optimized conflict detection using `HashSet` for $O(1)$ component lookups and added support for custom task-level concurrency control.
    - **ECS Optimization**: Optimized `EntityArchetype.containsAll` to avoid $O(N)$ `BitSet` cloning during entity queries.
    - **ParallelTask Flexibility**: Extended `ParallelTask` with `conflictsWith` method, allowing non-ECS tasks to define their own conflict logic for better parallelization.

### Added
- **SimCity Expansion**:
    - Introduced **Agricultural Zone** (`AGRICULTURAL`) to the SimCity plugin with dedicated costs, income, and maintenance profiles.
    - Expanded **Infrastructure Systems**: Added **Railways**, **Metro**, **Water Supply** (`WATER_PIPE`), and **Electricity** (`POWER_PLANT`, `POWER_LINE`).
    - Implemented **Electricity Propagation**: Power spreads through continuous construction (adjacent buildings) or dedicated power lines. Infrastructure (roads/pipes) does not conduct electricity.
    - Updated `PopulationService`: Residents now require both water and power; lack of power causes severe happiness penalties (-30).
    - Updated `EconomyService`: Commercial and Industrial zones generate 50% less income if not powered. Added maintenance for power infrastructure.
    - Added visualization support for all new types (`A` for Agricultural, `=` for Rail, `M` for Metro, `w` for Water, `P` for Power Plant, `z` for Power Line) in `CityConsoleView`.
    - Implemented **Pollution System**:
        - **Air Pollution**: Generated by industrial zones, power plants, and traffic (roads). Spreads to neighbors and dissipates naturally.
        - **Water Pollution**: Generated by industrial and agricultural zones.
        - **Happiness Impact**: High pollution levels now significantly reduce resident happiness.
- **Comprehensive Boundary Testing**:
    - Implemented `SimCityBoundaryTest` covering:
        - **Economic Boundaries**: Exact zero-balance building and cost-minus-one failure cases.
        - **Spatial Boundaries**: Corner validation (0,0 and max coordinates) and out-of-bounds failure checks.
        - **Density Boundaries**: Prevention of building overlap (collision detection).
        - **Social/Logic Boundaries**: Happiness impact of 100% tax rates, revenue loss at 0% tax, and impact of infrastructure availability (Water/Metro/Power).
        - **Electricity Logic**: Verification of power spread through buildings/lines and blockage by empty space or roads.
        - **Pollution Logic**: Verification of pollution generation, spread, and impact on residents.

### Changed
- **SimCity Service Refinement**: 
    - Updated `EconomyService` to handle agricultural production and maintenance.
    - Refactored `PopulationService` happiness calculation to support new environmental factors.

## [1.32.0] - 2026-05-09

### Added
- **ArchUnit Enforcement**: Added strict rules to `ArchitectureTest` to prevent plugins from accessing internal engine classes (marked with `@InternalEngine` or residing in `engine.parallel`).
- **Engine API Marking**: Applied `@EngineAPI` and `@InternalEngine` to all core engine classes and interfaces.

### Changed
- **JPMS Hardening**: Updated `island-engine/module-info.java` to stop exporting the `com.island.engine.parallel` package, completing the encapsulation of internal threading logic.
- **Annotation Retention**: Changed `@EngineAPI` and `@InternalEngine` retention policy to `CLASS` to allow ArchUnit to verify architectural constraints via bytecode analysis.
- **Test Alignment**:
    - Relocated `WorldInitializationTest` to `com.island.nature.integration` package within the `island-nature` module, resolving a package-level architectural violation.
    - Refactored `SimCitySmokeTest` and `SimCityCoreLogicTest` to use the public `SimulationEngine.build()` API, eliminating dependency on internal engine schedulers.
- **Economy System**: Added explicit `UnsupportedOperationException` to the placeholder `EconomySystem.process()` method to prevent silent logic gaps.

### Fixed
- **SimCity Growth Logic**: Fixed a regression in `SimCitySmokeTest` where the introduction of `ConnectivityService` and `CityAnalyticsService` prevented population growth due to missing infrastructure (roads/industrial demand).

## [1.31.0] - 2026-05-08

### Added
- **API Stability**: Introduced `@EngineAPI` and `@InternalEngine` annotations to clearly demarcate the public engine contract from internal implementation details.
- **JPMS Support**: Added `module-info.java` to `island-engine` to enforce module boundaries and proper encapsulation.
- **Cross-Module Architecture Test**: Expanded `ArchitectureTest` in `island-app` to enforce strict isolation between engine, util, and domain modules.

### Changed
- **Modularization Finalized**: Successfully transitioned to a full multi-module Maven structure. Deleted the redundant root `src` tree and organized development scripts into `scripts/`.
- **Engine Purity**: Removed domain-specific events (`EntityBornEvent`, `EntityDiedEvent`) from the core engine package to ensure zero domain knowledge in the infrastructure layer.
- **SimCity ECS Stabilization**: 
    - Fixed numerous compilation and logic errors in the `island-simcity` module following its ECS migration.
    - Improved thread-safety in `CityMap` using `AtomicLong` for financial state.
    - Implemented bankruptcy threshold logic (5 ticks of negative balance).
- **Nature Domain Refinement**: Refactored `ConsumableComponent` and `ConsumeAction` to use a typed `Cell` context, eliminating the last remaining `instanceof` check in the feeding system.
- **Engine Optimization**:
    - Refactored `DefaultWorkUnit` to extend `AbstractList`, removing 60+ lines of redundant delegation code.
    - Improved `PhaseScheduler` cache invalidation logic using `identityHashCode` and structural list checks.

### Fixed
- **Thread Safety**: Resolved a TOCTOU (Time-of-Check to Time-of-Use) race condition in `GameLoop.stop()`.
- **Log Semantics**: Updated `NatureLauncher` stop condition logs to accurately reflect current simulation logic (total animal extinction).
- **Test Alignment**: Moved `GameLoopOptimizationTest` to the correct module (`island-engine`).

## [1.30.0] - 2026-05-08

## [1.29.0] - 2026-05-08

### Added
- **Dynamic River Generation**: Updated `WorldInitializer` to generate a 3-cell wide winding river across the island. This acts as a natural barrier for slow land animals (speed 1-2) while remaining accessible to fast animals (speed 3-4), aquatic animals (ducks, frogs), and flying animals (eagles).

### Changed
- **Species Capabilities**: Updated `duck` properties to include `canFly=true`, allowing it to navigate over any terrain.

### Fixed
- **Test Suite Stability**: Resolved multiple `NullPointerException` failures in `ReproducibilityTest`, `SimulationOptimizationTest`, and `AnimalHealthSystemTest` by correctly injecting `DefaultClimateService` into the `NatureDomainContext` builder.

## [1.28.0] - 2026-05-08

### Added
- `ConsumeAction<T>` functional interface for type-safe resource consumption.
- SimCity ECS Components: `PopulationComponent`, `BuildingComponent`, `EconomyComponent`.

### Changed
- `ConsumableComponent` refactored to use generics `ConsumableComponent<T>` for better type safety and to eliminate `instanceof` checks.
- Migrated SimCity to "Pure ECS" architecture:
    - Removed `Resident` and `Building` subclasses.
    - `SimEntity` now implements `Entity` interface and uses `ComponentStore`.
    - All SimCity services (`PopulationService`, `EconomyService`, etc.) migrated to `EntitySystem` and refactored to use components instead of `instanceof`.
- Updated `SimCityPlugin` and `CityMap` to support the new ECS-based SimCity domain.

## [1.27.0] - 2026-05-08
### Changed
- **Documentation Overhaul**: Centralized architectural and project information. Removed redundant documents (`codereview.md`, `TODO.md`), consolidated information into `PROJECT_CONTEXT.md` and `DOCUMENTATION.md` for better maintainability and clarity, and updated the roadmap for the upcoming multi-module architecture migration.
- **Architectural Refinement**: Verified architecture against Review v7 recommendations. Added roadmap for multi-module maven structure, API stability (via `@EngineAPI`), and module isolation.

## [1.26.0] - 2026-05-07
### Added
- **Headless Mode**: Introduced `headless` configuration flag and `HeadlessView` to allow running simulations without visualization. Optimized `TaskRegistry` to skip expensive world snapshots when in headless mode, improving benchmark performance and enabling cleaner CI/CD integration.
- **CLI Support**: Added `--headless` argument to `NatureLauncher`.

### Fixed
- **CRITICAL: Deadlock in Cell Iteration**: Resolved a major deadlock issue in `Cell.java` where `ReadWriteLock` read-to-write upgrades were attempted during entity iteration (e.g., during movement or feeding). All iteration methods (`forEachEntity`, `query`, `forEachAnimalSampled`, etc.) now use local copies to release the read lock before executing actions, ensuring thread safety and preventing simulation hangs.
- **Task Scheduling Safety**: Improved `TaskRegistry` to conditionally register the view task based on the visualization mode.

## [1.25.0] - 2026-05-07
### Added
- **Climate System**: Introduced `ClimateService` (and `DefaultClimateService`) to manage global seasons and temperature in the `PREPARE` phase.
- **Temperature-Driven Growth**: Updated `BiomassGrowthSystem` to scale plant growth based on current temperature (frozen/heat stress modifiers).
- **Thermal Metabolism**: Enhanced `AnimalHealthSystem` with temperature-dependent metabolism. Cold-blooded animals hibernate/slow down in cold; warm-blooded animals consume more energy in extremes to maintain homeostasis.

### Optimized
- **Memory Footprint**: Refactored `EntityContainer` to use compact `ArrayList` buckets and eliminated multiple redundant `Set` indices, significantly reducing memory usage for extreme-scale simulations (millions of entities).
- **Fast Initialization**: Added "silent" mode to `Cell.addAnimal` to bypass the event bus during initial world population, preventing `OutOfMemoryError` and GC thrashing during startup.
- **Component Storage**: Reduced `ArrayComponentStore` overhead by using lazy array initialization.
- **Locking Overhead**: Removed internal `energyLock` from `Organism` by utilizing the thread-safety guarantees of the phase-based execution graph and cell-level locks.
- **Partitioning Strategy**: Fixed a bottleneck where dynamic chunking wasn't applied after initial population, ensuring parallel efficiency from the first tick.

### Fixed
- **Log Spam**: Silenced individual death logging in `AlertService` for common hunger deaths, preserving debug logs for critical system events only.
- **Benchmark Stability**: Tuned `ExtremeScalePerformanceTest` to run reliably on standard hardware (4GB heap) with a 20x20 grid (4M+ entities).

## [1.24.0] - 2026-05-07
### Added
- **Dynamic Load Balancing**: Introduced `DynamicChunkingStrategy` which adaptively partitions the world based on entity density (recursive splitting).
- **Engine Telemetry**: Added `WorkUnit` interface and instrumented `ParallelDispatcher` to measure actual execution time per work unit.
- **Generic WorkUnits**: Added `DefaultWorkUnit` for non-specialized domains (e.g., SimCity).
- **Periodic Rebalancing**: Added `rebalance()` hook to `SimulationWorld` and integrated it into `Island` tick lifecycle (configurable interval).

### Changed
- **NaturePlugin Refactoring**: Extracted `NatureDomainContextFactory` to handle complex domain assembly, simplifying the plugin constructor.
- **Architecture Enforcement**: Decoupled engine's parallel infrastructure from domain-specific `Chunk` model using the `WorkUnit` abstraction.

### Fixed
- **FeedingMechanicsTest**: Resolved NullPointerException caused by missing `ComponentRegistry` stubbing.
- **Test Suite Stability**: Updated 12+ test files to include mandatory `ChunkingStrategy` in `NatureDomainContext` builder.
- **Checkstyle Compliance**: Fixed formatting in several core classes to meet strict project standards.

## [1.23.0] - 2026-05-07
### Added
- **System Execution Graph**: Implemented `SystemExecutionGraph` with static dependency resolution and parallel grouping based on read/write component sets.
- **PhaseScheduler Optimization**: Added schedule caching to `PhaseScheduler`, avoiding redundant graph traversals when the task list is unchanged.
- **Config-Driven Partitioning**: Moved all chunking magic numbers from `Island.java` to the `Configuration` system.
- **Lifecycle Guarantees**: Unified simulation termination logic via `onStopCallback` in `GameLoop`, ensuring `onSimulationStopped` is always called.

### Fixed
- **GC Allocation Reduction**: Optimized `SystemExecutionGraph.buildSchedule` to use list-based conflict detection, eliminating `HashSet` churn in the hot path.
- **Code Standards**: Removed dead `process()` overrides in ECS systems (`AnimalReproductionSystem`) and cleaned up FQNs in `PhaseScheduler`.

## [1.22.0] - 2026-05-07
### Added
- **Typed Events**: Introduced `AnimalBornEvent` and `AnimalDiedEvent` to the core engine. These provide type-safe lifecycle notifications, enabling `StatisticsService` and `AlertService` to handle events without `instanceof` checks.
- **Specialized ECS Systems**: Split the monolithic `HealthSystem` and `MovementSystem` into specialized variants: `AnimalHealthSystem`, `BiomassGrowthSystem`, `AnimalMovementSystem`, and `BiomassMovementSystem`. This ensures each system handles only entities with compatible components, adhering to the Open/Closed Principle.
- **Type-Safe Nature API**: Added `getCell` and `moveOrganism` to `NatureWorld` and `Island` to provide a type-safe API for nature-domain operations, eliminating the need for `SimulationNode` to `Cell` narrowing in most systems.

### Changed
- **ECS Registry Isolation**: Refactored `ComponentRegistry` from a global static singleton to an instance-based registry. This allows multiple simulation instances to run in isolation with their own component indexing.
- **Stop Condition Semantics**: Updated `NaturePlugin.shouldStop` to trigger on total animal extinction (excluding biomass) instead of individual species extinction, providing a more stable end-game state for large simulations.
- **Safe Component Storage**: Updated `ArrayComponentStore` to support dynamic array growth, preventing silent component loss when exceeding initial capacity.
- **Contract Simplification**: Removed the redundant `process(T, int)` method from the `EntitySystem` interface to resolve signature conflicts with the optimized parallel execution path.
- **Dependency Injection**: Updated `Organism`, `AnimalFactory`, and `WorldInitializer` to receive `ComponentRegistry` via constructor injection, furthering the shift away from global state.
- **Node Type Narrowing Refactoring**: Centralized `instanceof Cell` checks in `AbstractService` using the Template Method pattern. Updated `Biomass`, `SwarmOrganism`, and `PreyProvider` to use `Cell` directly, improving domain type safety.

### Fixed
- **Simulation Isolation**: Fixed potential index collisions and state leakage between simulation runs caused by the static registry.
- **Test Stability**: Resolved multiple "cannot find symbol" and compilation errors in the test suite resulting from the system refactoring and registry changes.
- **Movement Logic Robustness**: Improved `AnimalMovementSystem.selectTargetCell` to be more reliable for small grids and high speeds, resolving flakiness in `AnimalMovementSystemTest`.

## [1.21.0] - 2026-05-06
### Added
- **ECS Infrastructure**: Introduced `ComponentStore` (Default and Array implementations) and `EntityQuery` for high-performance component management.
- **ECS Systems**: Migrated `LifecycleService` and `MovementService` to `HealthSystem` and `MovementSystem` using the ECS System pattern.
- **Verification**: Added `HealthSystemTest` and `MovementSystemTest` for comprehensive verification of the new architecture.

### Changed
- **Organism Refactoring**: Refactored `Organism` to use `ComponentStore` instead of direct `Map` for components, optimizing lookup and memory usage.
- **Biomass Mobility**: Updated `Biomass` to support `MovementComponent`, enabling unified movement logic through the ECS system.

### Removed
- **Deprecated Services**: Removed `LifecycleService` and `MovementService` along with their associated tests.

## [1.20.0] - 2026-05-06
### Fixed
- **Test Suite Stability**: Fixed "cannot find symbol" errors across multiple test files (`TrophicFeedingTest`, `GameLoopOptimizationTest`, `FeedingMechanicsTest`, `ArchitectureTest`, `ChameleonTest`). Standardized static imports for Mockito, JUnit 5, and ArchUnit, and resolved missing dependency injections in test constructors.
- **HF-1: Centralized Death Reporting**: Standardized `MovementService` and `ReproductionService` to use `die(cause)` for explicit state management. Verified that all services have shifted from direct `EventBus` publication to centralized reporting via `Island.onEntityRemoved`, eliminating double-counting of deaths.
- **Species Registry Cleanup**: Removed redundant `SpeciesKey` and optimized `SpeciesLoader` for better maintainability and performance.
- **Herbivore Lifecycle**: Modernized `Butterfly` and `Caterpillar` lifecycle management with improved trait and state handling.
- **Test Coverage**: Integrated `SimulationStopConditionTest` and `StatisticsDeathCountingTest` to improve simulation verification.

### Changed
- **InteractionMatrix**: Simplified matrix handling to reduce complexity and potential bugs.
- **Refactoring**: Applied general refactoring and cleanup across `NaturePlugin` and entity factories to align with project standards.

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
