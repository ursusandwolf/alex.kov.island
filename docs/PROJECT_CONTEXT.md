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

- **Security, Persistence & Domain Hardening (May 15, 2026) - COMPLETED**:
    - **Thread-Safety**: Resolved race condition in `SocialService` by using `AtomicInteger` accumulators in `CityTile`. Fixed visibility issues in `SimulationProperties`.
    - **Refactoring**: Decoupled building effects in `SocialService` using `SocialEffectProvider` strategy pattern, achieving full OCP compliance.
    - **Security**: Integrated Spring Security with Basic Authentication to protect `/api/v1/simulation/**` and Actuator endpoints (except health/info).
    - **Persistence**: Migrated snapshot history from filesystem to JPA/H2 database, improving reliability and queryability.
    - **Quality**: Integrated Revapi for API compatibility checks and implemented jqwik property-based tests for `AnimalHealthSystem`.
    - **Ops**: Created multi-stage `Dockerfile` and `docker-compose.yml` with Prometheus monitoring.

## Architecture
- **Engine (Stable v1.55.0)**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling. Now protected by Revapi.
- **Nature (Stable v1.55.0)**: High-performance ecosystem with predatory and metabolic logic. Verified by property-based tests.
- **SimCity (Stable v1.55.0)**: Grid-based urban simulation with RCI zones. Refactored `SocialService` for extensibility and thread-safety.
- **App (Stable v1.55.0)**: Spring Boot-managed backend with Basic Auth, JPA persistence, and comprehensive observability (Actuator + Prometheus).

## Next Steps
- **Observability**: Implement Grafana dashboards using the provided Prometheus configuration.
- **Extension**: New domain-specific plugins (e.g., Space, Deep Sea) using the extensible `SimulationPlugin` and `SocialEffectProvider` APIs.
- **Quality**: Expand jqwik coverage to movement and reproduction systems.



