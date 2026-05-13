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
- **Code Quality Audit & Hardening (May 13, 2026) - COMPLETED**:
    - **Concurrency**: Resolved TOCTOU NPE race condition in `SimulationService` lifecycle methods, including `shutdown`.
    - **Performance**: Fully decoupled WebSocket STOMP broadcasting from simulation hot path using `AtomicReference` and `@Scheduled`.
    - **React UI**: Resolved O(W×H) rendering bottleneck in `App.tsx` using `useMemo` for coordinate-based indexing.
    - **Configuration**: Enabled background task scheduling via `@EnableScheduling`.
    - **Standards**: Refactored `NaturePlugin` to strictly follow Lombok standards (`@Getter`, `@AllArgsConstructor`, `@NoArgsConstructor`).
    - **Build Integrity**: Restored compilation and functionality of the simulation broadcasting system.
    - **Validation**: All tests passing, including isolated web-layer tests.
- **Maintenance & Plugin Hardening (May 13, 2026) - COMPLETED**:
    - **Plugin SPI**: Enhanced `NamedSimulationPlugin` with `withConfiguration` to support dynamic parameters for Spring-managed singleton plugins.
    - **Optional API**: Transitions to `Optional` in `SimulationService` and `SnapshotHistoryService` to eliminate `null` returns.
    - **Regression Fix**: Ensured `width`, `height`, and `snapshot` parameters are correctly passed to simulation engines.
- **Release v1.54.0 (May 12, 2026) - COMPLETED**:
    - **Stability**: Core (island-engine) audited and frozen.
    - **Fixes**: Map rendering issue resolved via JSON serialization fix.
    - **Packaging**: Verified JAR library structure and JPMS exports.

## Architecture
- **Engine (Stable v1.54.0)**: Decoupled core with SoA-based storage, phase-based scheduling, and robust thread pooling.
- **Nature (Stable v1.54.0)**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity (Stable v1.54.0)**: Grid-based urban simulation with RCI zones and environmental mechanics.
- **App (Stable v1.54.0)**: Spring Boot-managed backend providing REST and WebSocket APIs for simulation control and visualization.

- **Architecture Constraint (JPMS)**:
    - Attempted to refactor `SimulationService` to remove direct domain module dependencies (NaturePlugin).
    - Failed due to JPMS module isolation and classloader conflicts in integration tests, causing `ClassCastException` where `NaturePlugin` and `NamedSimulationPlugin` were loaded by different module loaders despite being in the same module path.
    - Decided to maintain explicit dependency to ensure test and runtime stability for current release.
    - Future refactorings must address classloader consistency in integration test contexts.

## Next Steps
- **Maintenance & Optimization (May 13, 2026)**:
    - **Backlog Grooming**: Consolidated technical debt and maintenance tasks into updated `todo.md`.
    - **Quality Roadmap**: Prioritizing Revapi integration, property-based testing, and mutation testing pipeline.
- **Monitoring**: Observability of the live nature simulation.
- **Extension**: New domain-specific plugins using the stable Engine API.

