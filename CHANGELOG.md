# Changelog

## [2026-04-26]
### Modernization & Scalability
- **Java 21 Migration**: Upgraded project from Java 17 to Java 21 LTS to leverage the latest JVM features and performance improvements.
- **Virtual Threads (Project Loom)**: Replaced traditional `FixedThreadPool` with `VirtualThreadPerTaskExecutor` in `GameLoop`. This allows for near-infinite scalability of parallel simulation tasks (chunks) without the overhead of platform threads.
- **Sequenced Collections**: Updated UI and processing logic to use Java 21's new collection interfaces (e.g., `getFirst()`, `getLast()`) for cleaner and more expressive code.
- **Maven Configuration**: Updated `pom.xml` properties and `maven-compiler-plugin` to target Java 21.

## [2026-04-25]
### Architecture & SOLID Refactoring
- **Interface Segregation (ISP)**: Deployed `Mobile`, `Consumer`, and `Reproducible` interfaces. Removed the bloated `OrganismBehavior` interface.
- **Dependency Inversion (DIP)**: Refactored `AnimalFactory` and all 15 animal species to use constructor injection for `AnimalType`. Removed hard dependencies on `SpeciesConfig` Singleton inside animal classes.
- **Bootstrap Pattern**: Introduced `SimulationBootstrap` and `SimulationContext` to decouple initialization logic from `SimulatorMain` (SRP).
- **Service Abstraction**: Created `AbstractService` to centralize parallel chunk processing logic, reducing code duplication across all simulation phases.

### Performance & Concurrency
- **Incremental Statistics**: Implemented `AtomicInteger` counters in `Island` for O(1) population tracking, replacing the expensive O(N²) full-grid scans.
- **Efficient Collections**: Updated `Cell` getters to return `UnmodifiableList` views instead of creating defensive copies on every call.
- **Thread-Safe Energy**: Synchronized `consumeEnergy` method to ensure atomic read-modify-write operations during parallel processing.
- **Tick Optimization**: Accelerated simulation speed 10x (tick duration reduced to 10ms) and optimized `GameLoop` thread pool configuration.

### Enhanced Biological Model
- **Plant Refactoring**: Moved plants to `com.island.content.plants`. Replaced anonymous classes with concrete `Grass` and `Cabbage` classes.
- **Biomass-based Feeding**: Plants now track biomass in kilograms. Herbivores consume specific amounts of biomass rather than whole plant objects.
- **Specialized Diet**: Rabbits, Goats, and Ducks now prioritize `Cabbage` for higher energy yield.
- **Natural Settlement**: Implemented probabilistic world initialization to create clusters and diverse population densities.

### Visualization & UI
- **Dynamic Map**: Implemented Top-3 species cycling in cells based on biomass to show "hidden" life on the map.
- **Population Sparklines**: Added real-time Unicode graphs (▂▃▅█) to the dashboard for visual trend analysis of every species.
- **UI Throttling**: Limited dashboard updates to every 5 ticks to ensure readability at high simulation speeds.

### Quality Assurance
- **JUnit 5 Suite**: Expanded test coverage to 14 tests including energy consumption, trophic feeding, concurrent movement, and world initialization.
- **Code Cleanup**: Removed all debug `System.out.println` calls and deleted obsolete classes like `TerminalTaskRegistry`.

## [2026-04-24]
### Multithreading & Performance
- **Island Partitioning**: Implemented 2x2 grid partitioning of the island into 4 Chunks for parallel processing.
- **Parallel Services**: Refactored `LifecycleService`, `FeedingService`, `MovementService`, `ReproductionService`, and `WorldInitializer` to use `ExecutorService` for parallel execution by chunks.
- **Thread-Safe Movement**: Implemented transactional move logic in `Island.moveOrganism` using ordered locking on cells to prevent deadlocks and race conditions.

### Biological Model Improvements
- **Trophic Hierarchy**: Implemented a priority system where predators act before their prey within a single tick.
- **Escape Protection**: Prey now has a chance to hide after a failed hunt, becoming invisible to other predators for the remainder of the tick.
- **Caterpillar Survival**: Added a "stealth mode" for caterpillars during the first tick of the simulation.
- **Dynamic Energy Management**: Volatile energy and life status fields for better thread visibility.
- **Offspring Scaling**: Implemented a per-pair reproduction model with mathematical progression based on animal weight and herbivore bonuses.

### Quality Assurance
- **TrophicFeedingTest**: Added specialized tests to verify hierarchical feeding, hiding mechanisms, and caterpillar protection logic.
- **Bug Fixes**: Fixed an issue where eaten prey was not correctly marked as dead, and improved test robustness for random events.


## [2026-04-23]
### Git Cleanup
- Deleted stale/merged local and remote branches (`feature/feeding-logic`, `feature/fox`, `optimization-to-flyweight-pattern-6fce0`, `feature/flyweight-optimization2`, and various `revert-` and non-English branches).
- Renamed `optimization-to-flyweight-pattern-6fce0` to `feature/flyweight-optimization` for better readability and conformity to standards.
- Synchronized local and remote branch tracking.
### Visualization & View
- Implemented `ConsoleView` for real-time island state monitoring using Unicode icons (🐺, 🐇, 🌿, etc.).
- Added per-species population statistics and a localized 10x5 map fragment for visual feedback.

### Ecosystem Diversity
- Restructured animal packages: grouped species into `predators` and `herbivores` subpackages.
- Introduced `Predator` and `Herbivore` marker interfaces to classify animal types.
- Updated `AnimalFactory` to support the new package structure.
- Expanded animal variety to full requirements (15 species: Wolf, Boa, Fox, Bear, Eagle, Horse, Deer, Rabbit, Mouse, Goat, Sheep, Boar, Buffalo, Duck, Caterpillar).

### Running the Application
The simulation can be started from the project root using:
`mvn clean compile exec:java`
The main entry point is now `com.island.Main`.

### Engine & Core Implementation
- Migrated `GameLoop` to `ScheduledThreadPoolExecutor` for precise tick control.
...

- Created `LifecycleService` to handle aging, basal metabolism, and death of organisms.
- Implemented `WorldInitializer` to separate world setup from model.
- Added `PlantGrowthService` for handling plant reproduction as a recurring phase.
- Introduced `TerminalTaskRegistry` for one-time event processing.
- Implemented `Configuration` and `InteractionMatrix` for data-driven organism behavior.
- Developed behavior services: `FeedingService` (with energy gain), `MovementService`, and `ReproductionService`.

### Model Refactoring
- Simplified energy system: energy now corresponds to food weight units.
- Cleaned up `Cell` class: now a pure container with thread-safe cleanup.
- Enabled automatic death via hunger (10% energy loss/tick) and age.
