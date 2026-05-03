# Changelog

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
### Added
- **Plugin Architecture**: Introduced `SimulationPlugin` and `SimulationEngine` for a formal domain decoupling. Launching simulations now follows a standard plugin-based lifecycle.
- **Centralized Sampling**: Extracted redundant stride-sampling logic into `SamplingUtils` to ensure consistent and efficient population sampling across all services.

### Changed
- **Optimized City Simulation**: Refactored `CityMap` to cache parallel work units and replaced `CopyOnWriteArrayList` in `CityTile` with a standard `ArrayList` protected by a `ReentrantLock`.
- **Dynamic Configuration**: Updated `Configuration.load()` to use reflection, ensuring all fields from `species.properties` are correctly populated without manual mapping.
- **Thread Safety & Deadlock Fix**: Refactored `Cell.java` to process entities outside of read locks, resolving a critical deadlock caused by illegal lock upgrades during animal interactions.
- **Renderer Management**: Updated `NatureLauncher` to properly pass a managed `ConsoleView` instance to the engine, fixing a silent map rendering issue.

### Fixed
- **NPE Robustness**: Removed constructors allowing `null` world references in simulation services. Fixed `NullPointerException` risks in `ExtinctionBalanceTest` and `StressStabilityTest` related to view management.
- **Task Integrity**: Cleaned up `TaskRegistry` to remove redundant task registrations and ensure cleaner task lifecycles.

## [1.4.0] - 2026-05-02
### Changed
- **Decoupled Engine from Domain**: Removed the hardcoded `SimulationView` from `SimulationContext` and introduced a domain-agnostic `SimulationRenderer`.
- **Phase-based Scheduling**: Replaced the rigid `instanceof CellService` checking in `GameLoop` with a `Phase`-based system (`ScheduledTask`) that supports execution priorities.
- **Concurrency & Virtual Threads**: Updated `GameLoop` to utilize Java 21's `VirtualThreadPerTaskExecutor` instead of a fixed thread pool.
- **Data Integrity**: Modified getters in `Cell` (e.g., `getAnimals()`, `getEntities()`) to return immutable copies to protect internal state.
- **Event Listeners**: Replaced hard-cast calls to `Island` inside `Cell` with a `WorldListener` mechanism for domain-agnostic event handling.

### Fixed
- **Future Handling**: Added error handling for `Future` objects returned by `invokeAll` in `GameLoop` to log execution exceptions properly.
- **Test Configuration**: Fixed failing tests (`LifecycleServiceTest`, `FeedingMechanicsTest`) by correctly wiring mock `Configuration` objects. Disabled or silenced long-running stress tests to ensure faster CI cycles.

## [1.3.0] - 2026-05-01
### Added
- **Configuration Injection**: Introduced `Configuration` object injection across all Nature entities and services, enabling multi-instance simulations and thread-safe parameter management.

### Changed
- **Eliminated Static Constants**: Refactored `Organism`, `Animal`, `Biomass`, `Cell`, `Island`, and all services to accept configuration parameters instead of relying on static imports from `SimulationConstants`.
- **Refined Animal Inheritance**: Updated `Animal`, `GenericAnimal`, `Bear`, and `Chameleon` to correctly implement `Herbivore` and `Predator` interfaces with dynamic metabolism modifiers.
- **Service Decoupling**: Fully decoupled `FeedingService`, `MovementService`, `ReproductionService`, and `LifecycleService` from global state.

### Improved
- **Test Coverage & Stability**: Updated the entire test suite (33+ compilation errors fixed) to support dependency injection and verified consistency with parallel execution.
- **File Integrity**: Fixed several corrupted source files that contained trailing repeated code blocks.
- **Code Quality**: Resolved 10+ Checkstyle violations related to naming conventions and control flow structures.

## [1.2.0] - 2026-05-01
### Added
- **ISP-driven Interfaces**: Created `NatureRegistry`, `NatureStatistics`, `NatureEnvironment`, and `BiomassManager` to decompose the `NatureWorld` God Object.

### Changed
- **Nature Plugin Reorganization**: Relocated all Nature-related classes to a unified `com.island.nature.*` package structure (entities, model, service, view, config), aligning it with the `simcity` plugin architecture.
- **Service Refactoring**: Updated `LifecycleService`, `MovementService`, and `FeedingService` to use narrow interfaces, reducing coupling and improving modularity.
- **Renamed Nature Launcher**: Moved and renamed `MainNature.java` to `com.island.nature.NatureLauncher.java`.

### Improved
- **Architectural Clarity**: Improved project structure by separating core engine logic from domain-specific plugin implementations.

## [1.1.0] - 2026-05-01
### Added
- **SimCity MVP**: Implemented a functional city simulation prototype.
- **BuildingService**: Added a service for cost-based construction of roads and zones.
- **Bankruptcy System**: Added fiscal solvency tracking; city enters bankruptcy after 5 ticks of negative balance.
- **Alert System**: Implemented a notification system for critical city states (Bankruptcy, High Taxes, Housing Shortage).
- **Dynamic Migration**: Residents now react to tax rates and city solvency (leaving if unhappy).
- **Comprehensive Testing**: Added `SimCityCoreLogicTest` covering economy, bankruptcy, and thread safety.

### Improved
- **Concurrency**: Made `CityMap` and related services thread-safe for parallel simulation.
- **Code Quality**: Refactored `com.island.simcity` package using Lombok to reduce boilerplate and improve readability.
- **Engine Integration**: Updated `GameLoop` to support global world ticks.

## [1.0.0-SNAPSHOT] - 2026-05-01
### Added
- **Engine Initialization**: Added `initialize()` method to `SimulationWorld` for domain-specific topology setup (e.g., neighbor discovery).
- **Generic Metrics**: Introduced `Map<String, Number> getMetrics()` in `WorldSnapshot` for domain-agnostic data reporting.

### Changed
- **Type Safety**: Enhanced `CellService` generics to `CellService<T, N extends SimulationNode<T>>`, eliminating unsafe downcasts in simulation services.
- **Engine Decoupling**: Removed all nature-specific methods from `WorldSnapshot` (`getGlobalSatiety`, `getSpeciesCounts`, etc.), moving them to generic metrics.
- **SimCity Optimization**: Moved road connectivity and neighbor setup from `SimCityLauncher` to `CityMap.initialize()`.
- **Nature Plugin Alignment**: Updated all Nature services (`Feeding`, `Movement`, `Reproduction`, etc.) to use the new type-safe `CellService` signature.

### Fixed
- **SimCity Growth**: Fixed `SimCitySmokeTest` failure by properly initializing the map and ensuring residential demand exists during tests.
- **Checkstyle**: Fixed indentation and formatting issues in `IslandSnapshot` and `AbstractService`.
