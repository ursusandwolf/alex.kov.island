# Project Context

## Current State
- **Phase 4: User Interface, Controls & Persistence (IN PROGRESS)**:
    - **Spring Boot Integration**: Successfully transformed the application into a Spring Boot backend.
    - **REST API v1**: Implemented `/api/v1/simulation` endpoints for lifecycle management.
    - **WebSocket (STOMP)**: Implemented `SnapshotBroadcaster` for real-time world updates via `/topic/snapshot`.
    - **Engine Orchestration**: `SimulationService` manages the `SimulationEngine` and plugins within the Spring context.
- **Phase 5: Production Readiness & Quality Hardening (Completed)**:
    - **GitHub Actions CI**: Automated pipeline established for all modules with enforced JaCoCo coverage (75% for engine).
    - **SoA Correctness**: StampedLock unification and immutable snapshots verified.

- **Phase 4: User Interface, Controls & Persistence (In Progress)**:
    - **App Module & JPMS**: 
        - Fixed `ServiceLoader` plugin discovery by providing proper `module-info.java` exports in domain modules.
        - `island-app` successfully compiles and runs CLI visualizations for loaded domains.
    - **Spring Boot Readiness**: 
        - Implemented `pause()`/`resume()`/`getStatus()` in `GameLoop`.
        - Refactored `IslandSnapshot` for thread-safe immutability.
        - Configured Jackson Mixins in `island-app` for polymorphic serialization.
    - **Architecture**: Validated `SimulationEngine` for safe integration into Spring container.

## Architecture
- **Engine**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones and environmental mechanics.
- **App**: Spring Boot-managed orchestrator for simulation control and visualization (Readiness achieved).

## Next Steps
- **Persistence**: Implement saving and loading world snapshots to JSON/Database.
- **UI Enhancements**: Add entity details on cell click in `WorldCanvas`.
- **Configuration**: Allow custom simulation parameters (width, height, tick rate) from the dashboard.

