# Project Context

## Current State
- **Phase 4: User Interface, Controls & Persistence (COMPLETED)**:
    - **Spring Boot Integration**: Successfully transformed the application into a Spring Boot backend using recommended architectural patterns (Profiles, Customizers, Bean-based lifecycle).
    - **REST API v1**: Implemented `/api/v1/simulation` endpoints (pause, resume, status, snapshot) using `ResponseEntity` and Records.
    - **WebSocket (STOMP)**: Implemented `SimulationBroadcaster` with dynamic interval and automatic lifecycle registration.
    - **Engine Orchestration**: `SimulationService` manages the `SimulationContext` and lifecycle via Spring events.
- **Phase 5: Production Readiness & Quality Hardening (Completed)**:
    - **GitHub Actions CI**: Automated pipeline established for all modules.
    - **Quality Gate**: JaCoCo coverage threshold increased to 65% project-wide.
    - **Dependency Management**: Centralized versions for logback, jakarta-annotation, etc. in parent POM.
    - **JPMS Hardening**: Correctly configured module boundaries and exports/opens for Spring and Jackson.

- **Phase 4: User Interface, Controls & Persistence (Completed)**:
    - **App Module & JPMS**: 
        - Fully transitioned to Spring Boot architecture.
        - Resolved JPMS visibility and reflection issues for Jackson and Spring.
    - **Spring Boot Readiness**: 
        - Implemented `pause()`/`resume()`/`getStatus()` in `GameLoop`.
        - Refactored `SimulationService` and `SimulationController` for Spring-native patterns.
        - Configured Jackson Mixins via `Jackson2ObjectMapperBuilderCustomizer`.
    - **Architecture**: Validated the entire stack with integration tests.

## Architecture
- **Engine**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones and environmental mechanics.
- **App**: Spring Boot-managed backend providing REST and WebSocket APIs for simulation control and visualization.

## Next Steps
- **Persistence**: Implement saving and loading world snapshots to JSON/Database.
- **UI Enhancements**: Add entity details on cell click in `WorldCanvas`.
- **Configuration**: Allow custom simulation parameters (width, height, tick rate) from the dashboard.

