## [1.55.0] - 2026-05-13
### Added
- Frontend testing suite using Vitest and React Testing Library, including tests for `WorldCanvas` and `useSimulationStore`.
- `NamedSimulationPlugin` SPI with `withConfiguration` method for factory-based plugin instantiation.
- `GlobalExceptionHandler` for centralized REST API error handling.
- Bean Validation (`@Validated`, `@Min`, `@Max`) for `SimulationController` inputs.
- Modularized React components (`SimulationControls`, `SimulationMetrics`, `SnapshotHistoryPanel`).

### Fixed
- **Thread Safety**: Fixed TOCTOU NPE race conditions in `SimulationService` lifecycle methods (stop/pause/resume).
- **Performance**: Moved STOMP I/O broadcasting out of the simulation hot path in `SimulationBroadcaster` using `AtomicReference` and `@Scheduled`.
- **Performance**: Fixed O(W×H) frontend rendering bottleneck in `selectedNode` using `useMemo`.
- **Configuration**: Refactored `SimulationService` to properly inject `SimulationProperties` instead of scattered `@Value` fields.
- **Contract Enforcement**: Enforced factory pattern for `NamedSimulationPlugin.withConfiguration` by removing default `this` return, preventing singleton state pollution.
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
