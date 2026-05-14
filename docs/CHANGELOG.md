# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.55.0] - 2026-05-14

### Added
- Integrated **Spring Boot Actuator** with **Micrometer Prometheus** registry.
- Added no-args constructors and getters/setters to all `WorldSnapshot` implementations (`IslandSnapshot`, `CitySnapshot`, etc.) for robust polymorphic serialization.

### Changed
- Refactored `CitySnapshot` to be fully domain-agnostic by copying data from `CityMap` into serializable fields.
- Updated `SimulationBroadcaster` to use standard Spring property placeholder syntax (`${...}`) for scheduled tasks.
- Translated `Main.java` Javadoc to English to maintain codebase consistency.

### Fixed
- Resolved **JPMS** `InaccessibleObjectException` by opening `com.island.config` and domain model packages to required modules and reflection.
- Fixed a flaky integration test in `SimulationServiceIntegrationTest` by adding `await()` to ensure simulation start before lifecycle actions.
- Resolved polymorphic serialization failures in `SnapshotHistoryServiceTest` and enabled the test.
- Fixed `NaturePlugin` JPMS service provider requirement by adding a manual public no-args constructor.

## [1.54.0] - 2026-05-13

### Added
- Seeding simulation world from historical snapshots in `SimulationService`, `NaturePlugin`, and `SimCityPlugin`.
- Enhanced `SimulationBroadcaster` with dynamic tick interval and asynchronous STOMP broadcasting.
- New `SocialService` for Education and Health in `island-simcity`.
- Evolution mechanics for residents based on Education Quotient (EQ) and Health.
- High-Tech industrial zones transition logic.

### Changed
- Refactored `SimulationService` to use a plugin registry, eliminating if/else logic (OCP compliance).
- Cleaned up repository by removing tracked `node_modules` and updating `.gitignore`.
- Refactored `SimulationControllerTest` to use `@WebMvcTest` for faster execution.
- `SnapshotHistoryService.loadSnapshot` now returns `Optional<WorldSnapshot>`.

### Fixed
- Resolved TOCTOU NPE race condition in `SimulationService` lifecycle methods.
- Fixed React UI O(W×H) rendering bottleneck in the dashboard.
- Restored simulation broadcasting build integrity.

## [1.53.0] - 2026-05-12

### Added
- REST API v1 for simulation control and snapshot management.
- WebSocket STOMP broadcasting for real-time world state visualization.
- React-based Dashboard with World Canvas and Simulation Controls.
- `SnapshotHistoryService` for persistence of simulation states to JSON files.

### Changed
- Migrated application to Spring Boot framework.
- Centralized configuration using `SimulationProperties` and Spring Profiles.

## [1.50.0] - 2026-05-11

### Added
- Multi-threaded simulation engine with phase-based scheduling.
- Entity-Component-System (ECS) architecture with SoA storage.
- Nature simulation domain with metabolic and predatory logic.
- SimCity simulation domain with RCI zone mechanics and desirability.
- JPMS module isolation for core engine and domain plugins.
