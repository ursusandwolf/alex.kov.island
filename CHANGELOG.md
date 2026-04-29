# Changelog

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
