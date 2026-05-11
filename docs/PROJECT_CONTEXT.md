# Project Context

## Current State
- **Phase 5: Production Readiness & Quality Hardening (May 2026)**:
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
    - **Documentation Hardening**: Established Documentation Governance. Completed all documentation layers (P1-P3). Cleaned up redundant TODO files and consolidated the SimCity roadmap into `docs/simcity/ROADMAP.md`.
    - **Performance**: Optimized `ConnectivityService` to use `BitSet` for pathfinding and reduced GC pressure in several services.

## Architecture
- **Engine**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones, infrastructure, and environmental mechanics.

## Next Steps
- **Phase 4: User Interface, Controls & Persistence**:
    - Enhance existing terminal-based pseudographics (ASCII/ANSI) to render `WorldSnapshot` as rich as possible in CLI.
    - Build a Web-based (Spring Boot + React) dashboard in the `island-app` module for full graphical visualization.
    - Allow dynamic pausing, speed adjustments (tick rate scaling), and manual entity spawning via UI.
    - Implement serialization to save and load `WorldSnapshot` (JSON or binary formats).
