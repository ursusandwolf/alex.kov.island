# Project Context

## Current State
- **Phase 4: User Interface, Controls & Persistence (COMPLETED)**:
    - **Spring Boot Integration**: Successfully transformed the application into a Spring Boot backend using recommended architectural patterns (Profiles, Customizers, Bean-based lifecycle).
    - **REST API v1**: Implemented `/api/v1/simulation` endpoints (start, stop, pause, resume, status, snapshot) using `ResponseEntity` and Records.
    - **WebSocket (STOMP)**: Implemented `SimulationBroadcaster` with dynamic interval, listening to `SimulationStartedEvent`.
    - **Engine Orchestration**: `SimulationService` manages the `SimulationContext` dynamically, supporting custom width, height, and tick rate at runtime.
    - **UI Enhancements**: Added cell selection capability, displaying entity details on cell click in the `WorldCanvas` dashboard.
    - **Configuration**: Added dynamic UI controls for starting a simulation with custom parameters (width, height, tick rate).
    - **Persistence**: Implemented `SnapshotHistoryService` to save historical snapshots to JSON, with full support in the React dashboard.
- **Phase 5: Production Readiness & Quality Hardening (Completed)**:
    - **GitHub Actions CI**: Automated pipeline established for all modules.
    - **Quality Gate**: JaCoCo coverage threshold increased to 65% project-wide.
    - **Dependency Management**: Centralized versions for logback, jakarta-annotation, etc. in parent POM.
    - **JPMS Hardening**: Correctly configured module boundaries and exports/opens for Spring and Jackson.

## Architecture
- **Engine**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones and environmental mechanics.
- **App**: Spring Boot-managed backend providing REST and WebSocket APIs for simulation control and visualization.

## Next Steps
- **Documentation**: Finalize UI usage documentation and prepare for release.

