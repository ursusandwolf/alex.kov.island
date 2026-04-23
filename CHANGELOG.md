# Changelog

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
