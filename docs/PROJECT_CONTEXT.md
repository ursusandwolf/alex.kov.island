# Project Context

## Current State
- **Phase 4: User Interface, Controls & Persistence (COMPLETED)**:
    - **Spring Boot Integration**: Successfully transformed the application into a Spring Boot backend using recommended architectural patterns (Profiles, Customizers, Bean-based lifecycle).
    - **REST API v1**: Implemented `/api/v1/simulation` endpoints (start, stop, pause, resume, status, snapshot) using `ResponseEntity` and Records.
    - **WebSocket (STOMP)**: Implemented `SimulationBroadcaster` with dynamic interval, listening to `SimulationStartedEvent`.
    - **Engine Orchestration**: `SimulationService` manages the `SimulationContext` dynamically, supporting custom width, height, and tick rate at runtime.
    - **UI Enhancements**: Added cell selection capability, displaying entity details on cell click in the `WorldCanvas` dashboard.
    - **Configuration**: Added dynamic UI controls for starting a simulation with custom parameters (width, height, tick rate).
    - **Persistence**: Implemented `SnapshotHistoryService` to save historical snapshots to JSON, with full support in the React dashboard for saving, viewing, and reseeding simulations from history.
- **Phase 5: Production Readiness & Quality Hardening (Completed)**:
    - **GitHub Actions CI**: Automated pipeline established for all modules.
    - **Quality Gate**: JaCoCo coverage threshold increased to 65% project-wide.
    - **Dependency Management**: Centralized versions for logback, jakarta-annotation, etc. in parent POM.
    - **JPMS Hardening**: Correctly configured module boundaries and exports/opens for Spring and Jackson.

- **Documentation & Release Prep (Completed)**:
    - **UI Guide**: Created comprehensive `docs/UI_GUIDE.md` for the Web Dashboard.
    - **Maintenance**: Synced `todo.md` and `README.md` with current project architecture (Spring Boot migration).
    - **ADR Audit**: Finalized and accepted ADR 004 (Spring Boot Integration).
- **Engine Optimization (May 14, 2026) - COMPLETED**:
    - **Dispatcher**: Eliminated `ArrayList` allocations in `ParallelDispatcher` hot path by reusing pooled lists.
    - **Load Balancing**: Enhanced `DynamicChunkingStrategy` in `island-nature` to use `WorkUnit` execution time metrics for adaptive partitioning.
    - **Zero-GC**: Refactored `Cell` and `EntityContainer` to support allocation-free iteration. Replaced `Optional` with `getNodeOrNull` in critical movement logic.
- **Code Review & Quality Gate (May 14, 2026) - COMPLETED**:
    - **Infrastructure**: Added missing `micrometer-registry-prometheus` dependency and enabled Prometheus metrics.
    - **Modularity**: Resolved JPMS `InaccessibleObjectException` for configuration properties and service providers.
    - **Stability**: Fixed flaky `SimulationServiceIntegrationTest` lifecycle management.
    - **Persistence**: Fixed and enabled `SnapshotHistoryServiceTest` by implementing robust polymorphic serialization in all snapshot implementations.
    - **Consistency**: Unified scheduled task property resolution and translated `Main.java` Javadoc to English.

## Architecture
- **Engine (Stable v1.55.0)**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature (Stable v1.55.0)**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity (Stable v1.55.0)**: Grid-based urban simulation with RCI zones and environmental mechanics.
- **App (Stable v1.55.0)**: Spring Boot-managed backend providing REST and WebSocket APIs for simulation control and visualization.

- **Architecture Constraint (JPMS)**:
    - Attempted to refactor `SimulationService` to remove direct domain module dependencies (NaturePlugin).
    - Failed due to JPMS module isolation and classloader conflicts in integration tests, causing `ClassCastException` where `NaturePlugin` and `NamedSimulationPlugin` were loaded by different module loaders despite being in the same module path.
    - Decided to maintain explicit dependency to ensure test and runtime stability for current release.
    - Future refactorings must address classloader consistency in integration test contexts.

## Next Steps
- **Maintenance & Refactoring (May 15, 2026)**:
    - **SimCity**: Refactor `SocialService` to resolve OCP/SRP violations identified during code review.
    - **Quality Roadmap**: Implement Revapi integration and property-based testing (jqwik).
    - **Ops**: Create `Dockerfile` and `docker-compose.yml` for containerized deployment.
    - **Security**: Implement Spring Security (Basic Auth) for Actuator and control endpoints.
- **Extension**: New domain-specific plugins using the stable and optimized Engine API.


