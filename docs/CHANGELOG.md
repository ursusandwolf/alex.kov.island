# Changelog

## [1.6-SNAPSHOT] - 2026-05-01

### Added
- **SimCity Simulation Plugin**: Implemented a fully functional "City Builder" simulation as a proof-of-concept for engine modularity.
  - **Entities**: Added `Resident` (with age and happiness) and `Building` (Road, Residential, Commercial, Industrial).
  - **Models**: Created `CityMap` and `CityTile` as spatial implementations of `SimulationWorld` and `SimulationNode`.
  - **Services**:
    - `ConnectivityService`: Uses BFS to calculate road network connectivity from a city entrance.
    - `PopulationService`: Manages resident lifecycle, aging, and migration (attraction/repulsion based on happiness).
    - `EconomyService`: Handles tax collection and commercial/industrial income from connected tiles.
  - **Happiness Dynamics**: Residents react to environmental factors (pollution from industry, amenities from commerce, and infrastructure access).
  - **Console Visualization**: Added `CityConsoleView` to render the city map using ASCII symbols (`#`, `R`, `C`, `I`, `.` , `x`).
- **Plugin Verification**: Added `SimCitySmokeTest` and `SimCityRunnerTest` to ensure long-term stability and demonstrate engine extensibility.

## [1.5-SNAPSHOT] - 2026-05-01

### Added
- **Plugin Architecture**: Transformed the project into a modular platform with a generic simulation engine and a pluggable world implementation.
- **Generic Engine Core**: Refactored `SimulationWorld`, `SimulationNode`, and `GameLoop` to use generics (`<T extends Mortal>`), making them domain-agnostic.
- **NatureWorld Interface**: Introduced a bridging interface to allow nature-specific logic (statistics, registries) to extend the generic core.
- **Technical Documentation**: Added `MODULAR_ARCHITECTURE.md` detailing the new decoupled design and extension points.

### Changed
- **Domain Decoupling**: Completely removed all biology-specific imports from the `com.island.engine` package.
- **Inversion of Control**: Moved `TaskRegistry`, `SimulationBootstrap`, and `Season` from the engine to the `content` package. The engine now only hosts and executes tasks registered by the plugin.
- **LOD-Enabled Spatial Model**: Generalized `SimulationNode` to support broad entity iteration while maintaining domain-specific sampling methods in the `Cell` implementation.

### Fixed
- **Test Suite Alignment**: Updated all 43 tests to reflect the generic architecture, including mock adjustments for `NatureWorld` and fixed generic type signatures.
- **Hibernation Logic Consistency**: Corrected metabolism calculation in `LifecycleServiceTest` to align with the new seasonal scaling factors.

## [1.4-SNAPSHOT] - 2026-05-01

### Fixed
- **Plant Duplication Bug**: Fixed `SpeciesLoader` fall-through issue that caused plants to be incorrectly registered as animals.
- **Swarm Growth Violation**: Restored `maxBiomass` enforcement in `SwarmOrganism` to fix an LSP violation affecting `Butterfly` and `Caterpillar` growth logic.
- **Engine-Domain Coupling**: Completely eliminated domain layer leaks (`instanceof Cell`) from the simulation engine.
  - Expanded `SimulationNode` interface with necessary abstraction methods (`getRandomAnimalByType`, `removeEntity`, `getBiomass`, `addBiomass`, `cleanupDeadEntities`).
  - Implemented the interface in `Cell` using read/write locks for thread safety.
  - Refactored `FeedingService`, `LifecycleService`, `MovementService`, `ReproductionService`, `CleanupService`, and specific animal classes to use `SimulationNode` directly.
  - Removed remaining `instanceof Cell` checks from `GameLoop` during metrics aggregation by utilizing `node.forEachAnimal`.
- **Mutable Collection Leaks**: Secured `EntityContainer` by explicitly returning `Collections.unmodifiableSet` and `unmodifiableList` instead of leaking internal state through Lombok getters.

## [1.3-SNAPSHOT] - 2026-04-30

### Added
- **Dynamic Partitioning**: Implemented `partitionIntoChunks` in `Island.java` that adapts `chunkSize` based on world density and CPU core count. This ensures at least 16 tasks for small 8x8 worlds, eliminating the single-thread bottleneck.
- **Wolf Pack Coordination**: Refined `DefaultHuntingStrategy` to allow Wolf packs to hunt any animal over 150kg (including Bears) with a coordinate bonus of up to 30%.
- **Fox Special Ability**: Implemented **Agility Bonus** for Foxes, providing a 60% reduction in hunting energy costs. This allows the Fox to survive better in highly competitive ecosystems by attacking fast prey with minimal exhaustion.
- **Extinction Diagnostic Suite**: Added `ExtinctionBalanceTest` to track species survival and death causes across multiple simulation runs for scientific balancing.

### Optimized
- **Scalable Concurrency**: `SimulationBootstrap` now dynamically tunes `threadCount` (4 to `availableProcessors`) based on map size, maximizing core utilization across all hardware tiers.
- **Energy Buffering**: Increased `foodForSaturation` for predators (e.g., Fox) to act as an energy "buffer", reducing extinction rates caused by short-term hunt failures.

### Fixed
- **Buffalo Paradox Resolution**: Balanced the ecosystem by increasing Buffalo `settlementBase` (0.25) and `reproductionChance` (0.10) while tuning predator success rates to prevent boom-bust extinction cycles.
- **Founder Effect Fix**: Increased initial population density for low-reproduction giants to ensure viable breeding pairs at simulation start.

### Changed
- **Metabolic Scaling**: Tuned `BASE_METABOLISM_BP` to 100 BP (1% per tick) and added `HERBIVORE_METABOLISM_MODIFIER_BP` (5000) to sustain larger, more stable prey populations.

## [1.2-SNAPSHOT] - 2026-04-30
...
### Added
- **Parallel Task Grouping**: Refactored `GameLoop` to execute multiple `CellService` tasks (Feeding, Movement, Reproduction, etc.) in a single parallel pass per cell. This significantly reduces thread synchronization overhead and improves CPU cache locality.
- **Incremental Metrics Aggregation**: Integrated `SimulationMetrics` collection directly into parallel chunk processing. Global statistics are now aggregated on-the-fly, eliminating the need for expensive post-tick grid scans.
- **Metabolic Hibernation**: Introduced seasonal dormancy for cold-blooded species. In Winter, metabolism is reduced to 10% (`HIBERNATION_METABOLISM_MODIFIER_BP`), and entities skip intensive actions to ensure ecological stability.
- **CellService Interface**: Standardized business logic hooks (`beforeProcess`, `processCell`, `afterProcess`) to enable safe, grouped execution within the parallel engine.

### Optimized
- **O(1) Entity Cleanup**: Refactored `CleanupService` to interact directly with `EntityContainer` indices, allowing for constant-time removal of dead organisms and immediate factory recycling.
- **Zero-Scan Statistics**: `StatisticsService` now uses cached, pre-aggregated metrics from the parallel engine, providing O(1) access to global population and satiety data.
- **Seasonal Growth**: Updated `Biomass` logic to support variable growth rates controlled by seasonal modifiers.

### Changed
- **Engine Decoupling**: Moved finalization logic (metrics aggregation, world cleanup) out of `GameLoop` and into specialized services to improve modularity and testability.

## [1.1-SNAPSHOT] - 2026-04-30

### Added
- **Lombok Integration**: Replaced thousands of lines of boilerplate (getters, setters, builders) with Lombok annotations.
- **Integer Scaling**: Standardized energy/mass on `SCALE_1M` (long) and rates on `SCALE_10K` (int) for deterministic, high-performance arithmetic.
- **Enhanced Protection Map**: Optimized "Red Book" protection calculation to run once per tick, resulting in a 400x performance boost in crowded simulations.
- **Technical Manual**: Consolidated architectural "know-how" and biological logic into `README.md` for better maintainability.

### Fixed
- **Deadlock Prevention**: Verified and enforced consistent locking order (X then Y coordinate) in `Island` and `Cell` for multi-threaded movement.
- **Registry Pollution**: Fixed "Fall-through" bug in `SpeciesLoader` that polluted animal registries with plant data.
- **Concurrent Discovery**: Migrated `SpeciesKey` registry to `ConcurrentHashMap` to safely handle runtime species discovery.

### Changed
- **Code Cleanup**: Stripped redundant fully qualified names (FQNs), obsolete Javadocs, and descriptive comments to improve signal-to-noise ratio.
- **Immutable Registries**: `SpeciesRegistry` now uses defensive copying to ensure map immutability during runtime.
- **Entity Storage**: `EntityContainer` now uses indexed buckets and `LinkedHashSet` for O(1) removals and stable iteration.

## [1.0-SNAPSHOT] - 2026-04-29

### Added
- **Engine-First Architecture**: Refactored the simulation core to use a high-performance tick-based scheduler.
- **Data-Driven Species**: Organism parameters (weight, speed, prey, etc.) are now fully driven by `species.properties`.
- **Statistics Service**: Decoupled population tracking from the world model; centralized birth, death (by cause), and population reporting.
- **Systematic LOD (Level of Detail)**: Implemented sampling logic in `AbstractService` to handle dense populations (millions of entities) without performance degradation.
- **Object Pooling**: Implemented `AnimalFactory` with pooling to minimize GC pressure during high-frequency reproduction/death cycles.
- **Spatial Caching**: Cells now cache their neighbors for faster movement and hunting logic.
- **Wolf Pack Hunting**: Coordinated hunting logic for wolves to take down large prey like bears.

### Fixed
- **Movement Statistics Bug**: Fixed a major bug where animal movement was incorrectly counted as a birth and a death wasn't registered properly.
- **Chameleon/Small Species Overpopulation**: Re-balanced reproduction rates and metabolism to prevent exponential growth of small rodents.
- **Predator Efficiency**: Increased hunting attempts and added a density bonus to help predators control large populations of prey.
- **Compilation Failures**: Resolved multiple test suite compilation errors after the architectural refactor.

### Changed
- **Java 21 Virtual Threads**: Switched to virtual threads in `AbstractService` for parallel cell processing.
- **Stricter Metabolism**: Increased base energy loss and movement costs to enforce survival pressure.
- **Interaction Matrix**: Optimized prey selection using a pre-calculated interaction matrix.
- **Checkstyle Compliance**: All control blocks (if/else) now use braces `{}` as per Google Style.
