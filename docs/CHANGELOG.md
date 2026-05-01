# Changelog

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
