# Changelog

## [1.1.0] - 2026-04-29
### Added
- **Engine-First Architecture**: Extracted core simulation logic into `com.island.engine` module.
- **Service Layer**: Introduced specialized services for simulation logic (`MovementService`, `FeedingService`, `ReproductionService`, `LifecycleService`, `CleanupService`).
- **Object Pooling**: Implemented `AnimalFactory` with `ObjectPool` to reduce GC pressure for high-frequency entities (Mice, Rabbits).
- **Tick Scheduler Optimizations**:
    - **Skip Ticks**: Cold-blooded animals (snakes, frogs, chameleons) now act less frequently (every 2-4 ticks), simulating slow metabolism.
    - **Level of Detail (LOD)**: Spatial sub-sampling for overpopulated cells. Limits processing to a representative sample of organisms per tick.
- **Spatial Indexing**: Added cached neighbor lookups in `Cell` and `Island` for O(1) adjacency access.
- **Dependency Injection**: Constructor-based DI for all providers (Random, Matrix, Factory).

### Changed
- **Performance**: Improved simulation speed by **30x** (500 ticks with 40k+ entities now takes ~22s instead of 11m).
- **Architecture**: Decoupled `Cell` from interaction logic. `Cell` is now a data container, while `Services` handle behavior.
- **Java 21**: Fully utilized Virtual Threads (Loom) for parallel service execution.

### Fixed
- Mass extinction bug where herbivores failed to recognize specific plant types as food.
- Population inflation bug where insects were processed as both `Biomass` and `Animal`.
- Thread contention issues by using deterministic locking order and regional work units.
