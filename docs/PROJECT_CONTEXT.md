# Project Context

## Current State
- **Phase 5: Production Readiness & Quality Hardening (Completed)**:
    - **GitHub Actions CI**: Automated pipeline established for all modules.
    - **Engine Concurrency Modernization**: Replaced unmanaged threads with robust `ExecutorService`.
    - **SoA Correctness**: Replaced `volatile` arrays with `AtomicLongArray` and `AtomicIntegerArray`.
    - **Engine Lifecycle Hardening**: Improved `SimulationContext.close()` to ensure clean thread termination.
    - **SimCity ECS Transition**: Migrated legacy `EconomyService` to `EconomySystem` (ECS).
    - **SimCity Refactoring**: Eliminated Lombok warnings and duplicate methods in `CityTile` and `CityMap`.
- **Phase 4: User Interface, Controls & Persistence (In Progress)**:
    - **Design**: ADR 004 drafted for Spring Boot integration (REST + WebSocket).
    - **Architecture**: Validated `SimulationEngine` for safe integration into Spring container.

## Architecture
- **Engine**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones and environmental mechanics.
- **App (Upcoming)**: Spring Boot-managed orchestrator for simulation control and visualization.

## Next Steps
- **Spring Boot Integration**:
    - Update Maven parent and `island-app` dependencies.
    - Implement `SimulationService` for engine orchestration.
    - Create `SimulationController` for RESTful management.
- **Web Dashboard**: Build a React-based dashboard for graphical visualization.
- **Persistence**: Implement JSON serialization for `WorldSnapshot`.

