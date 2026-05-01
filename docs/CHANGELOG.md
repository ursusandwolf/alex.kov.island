# Changelog

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
