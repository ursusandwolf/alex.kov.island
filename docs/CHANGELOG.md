## [1.55.0] - 2026-05-13
### Added
- `NamedSimulationPlugin` SPI with `withConfiguration` method for factory-based plugin instantiation.
- `GlobalExceptionHandler` for centralized REST API error handling.
- Bean Validation (`@Validated`, `@Min`, `@Max`) for `SimulationController` inputs.
- Modularized React components (`SimulationControls`, `SimulationMetrics`, `SnapshotHistoryPanel`).

### Fixed
- **Modularity**: Removed redundant domain imports in `SimulationService` to ensure strict module decoupling.
- **Race Condition**: Secured simulation context switching by clearing the context reference before destruction.
- **Validation**: Added missing `@Min`/`@Max` constraints for `tickMs` in `start-from-snapshot` endpoint.
- **Plugin Regression**: Fixed issue where dynamic simulation parameters (width, height, snapshot) were ignored by Spring-managed singleton plugins.
- Fixed entity rendering bug in `colors.ts` (species code case sensitivity).
- Resolved race conditions in `SimulationService` and `SimulationBroadcaster` using `volatile`.
- Fixed modularity issues by updating `module-info.java` for `jakarta.validation` and `spring-context`.

### Changed
- Refactored `SimulationService.getSnapshot()` and `SnapshotHistoryService.saveCurrentSnapshot()` to return `Optional`, eliminating `null` pointers.
- Refactored `SimulationService` to use a plugin registry, eliminating if/else logic (OCP compliance).
- Cleaned up repository by removing tracked `node_modules` and updating `.gitignore`.
- Refactored `SimulationControllerTest` to use `@WebMvcTest` for faster execution.
- `SnapshotHistoryService.loadSnapshot` now returns `Optional<WorldSnapshot>`.

## [1.54.0] - 2026-05-13
### Added
- Seeding simulation world from historical snapshots in `SimulationService`, `NaturePlugin`, and `SimCityPlugin`.
