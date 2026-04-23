# Changelog

## [2026-04-23]
### Git Cleanup
- Deleted stale/merged local and remote branches (`feature/feeding-logic`, `feature/fox`, `optimization-to-flyweight-pattern-6fce0`, `feature/flyweight-optimization2`, and various `revert-` and non-English branches).
- Renamed `optimization-to-flyweight-pattern-6fce0` to `feature/flyweight-optimization` for better readability and conformity to standards.
- Synchronized local and remote branch tracking.

### Engine & Core Implementation
- Migrated `GameLoop` to `ScheduledThreadPoolExecutor` for precise tick control.
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
