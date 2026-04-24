# Changelog

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
