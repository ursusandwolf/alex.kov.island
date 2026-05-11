# Project Context

## Current State
- **Phase 5: Production Readiness & Quality Hardening (In Review)**:
    - **Code Review v12**: Resumed review process to verify Spring Boot readiness fixes.
    - **GitHub Actions CI**: Automated pipeline established for all modules with enforced JaCoCo coverage (100% checks met).
    - **SoA Correctness & Performance**: Unified all SoA stores (`Health`, `Age`, `Movement`) on `StampedLock` with optimistic read patterns.
    - **Engine Coverage**: Increased `island-engine` line coverage to **75%** with robust unit and integration tests.
    - **Benchmarks**: Extracted JMH benchmarks to a dedicated `island-benchmarks` module.
    - **Engine Lifecycle Hardening**: Improved `SimulationContext.close()` and added `pause()`/`resume()` to `GameLoop`.
    - **Immutable Snapshots**: Refactored `IslandSnapshot` for thread-safe web serialization.
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
- **Spring Boot Core**:
    - Implement `SimulationService` for engine orchestration.
    - Create `SimulationController` for RESTful management.
- **Web Dashboard**: Build a React-based dashboard for graphical visualization.
- **WebSocket Feed**: Implement real-time snapshot broadcasting using the new immutable `WorldSnapshot`.

