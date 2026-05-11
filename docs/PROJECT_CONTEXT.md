# Project Context

## Current State
- **Phase 5: Production Readiness & Quality Hardening (May 2026)**:
    - **GitHub Actions CI**: Automated pipeline established for all modules.
    - **SoA Memory Safety**: Readers hardened against race conditions and OOBE during expansion.
    - **API Documentation**: 100% Javadoc coverage for `@EngineAPI` components.
    - **Plugin discovery**: Transitioned to JPMS `ServiceLoader` for simulation plugins.
    - **Performance Benchmarking**: Integrated JMH for continuous SoA vs Object performance monitoring.
    - **Engine Concurrency Modernization**: Replaced unmanaged threads and `CountDownLatch` in `GameLoop` and `ParallelDispatcher` with robust `ExecutorService.invokeAll()` and `submit()` calls. Internal engine classes locked down with `final`. Checked against 662 Checkstyle rules (now fully compliant).
    - **SoA Correctness**: Replaced `volatile` arrays with `AtomicLongArray` and `AtomicIntegerArray` in `HealthSoAStore` and `AgeSoAStore`.
    - **Movement SoA**: `MovementSoAStore` now uses `StampedLock` for thread-safe resizing and element access.
    - **CityTile Hardening**: All entity management methods in `CityTile` are now protected by a `ReentrantLock`.
    - **SimCity Environmental Systems**:
        - **Desirability Map**: Implemented `DesirabilityService` to calculate tile appeal based on infrastructure and pollution.
        - **Zoning Densities**: `BuildingComponent` now supports LOW, MEDIUM, and HIGH density.
        - **Wealth Tiers**: `PopulationComponent` supports POOR, MIDDLE, and WEALTHY levels.
        - **Progression Logic**: `ZoningService` handles building upgrades and wealth progression based on environmental factors.
    - **Scheduling Improvements**: `ConnectivityService`, `PollutionService`, and `DesirabilityService` moved to `Phase.PREPARE` for better data consistency.
    - **Engine Lifecycle Hardening**: Added explicit tests for `SimulationEngine` build/start phases. Refactored executor creation for better readability.
    - **SimCity ECS Transition**: Migrated legacy `EconomyService` to `EconomySystem` (ECS). Introduced `BuildingProfile` to manage economic constants centrally.
    - **Visualization Hardening**: Implemented "Rich ASCII" for SimCity, aligning it with Nature's visual standards. Added support for ANSI colors and emojis in `CityConsoleView`.
    - **TODO Cleanup**: Eliminated technical debt in the SimCity economy module.

## Architecture
- **Engine**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones, infrastructure, and environmental mechanics. Now fully ECS-driven for economy.

## Next Steps
- **Phase 4: User Interface, Controls & Persistence**:
    - **Spring Boot Integration**: Update `island-app` to include Spring Web and WebSocket starters.
    - **Web Dashboard**: Build a React-based dashboard for full graphical visualization of the world.
    - **Persistence**: Implement serialization to save and load `WorldSnapshot` (JSON or binary formats).
    - **Real-Time Controls**: Add REST endpoints for dynamic pausing and speed adjustments.
