## [1.55.0] - 2026-05-13
### Added
- `NamedSimulationPlugin` SPI in `island-engine` for dynamic plugin resolution.
- `GlobalExceptionHandler` for centralized REST API error handling.
- Bean Validation (`@Validated`, `@Min`, `@Max`) for `SimulationController` inputs.
- Modularized React components (`SimulationControls`, `SimulationMetrics`, `SnapshotHistoryPanel`).

### Fixed
- Fixed entity rendering bug in `colors.ts` (species code case sensitivity).
- Resolved race conditions in `SimulationService` and `SimulationBroadcaster` using `volatile`.
- Fixed modularity issues by updating `module-info.java` for `jakarta.validation` and `spring-context`.

### Changed
- Refactored `SimulationService` to use a plugin registry, eliminating if/else logic (OCP compliance).
- Cleaned up repository by removing tracked `node_modules` and updating `.gitignore`.
- Refactored `SimulationControllerTest` to use `@WebMvcTest` for faster execution.
- `SnapshotHistoryService.loadSnapshot` now returns `Optional<WorldSnapshot>`.

## [1.54.0] - 2026-05-13
### Added
- Seeding simulation world from historical snapshots in `SimulationService`, `NaturePlugin`, and `SimCityPlugin`.
