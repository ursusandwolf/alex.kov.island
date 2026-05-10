# Project Context

## Current State
- **Phase 5: Production Readiness & Quality Hardening (May 2026)**:
    - **SoA Correctness**: Replaced `volatile` arrays with `AtomicLongArray` and `AtomicIntegerArray` in `HealthSoAStore` and `AgeSoAStore` to ensure thread-safe element visibility. [DONE]
    - **ID Management**: Optimized `EntityIdManager` using `ConcurrentLinkedQueue` for lock-free ID recycling. [DONE]
    - **Module Hardening**: Refined JPMS isolation and integrated `revapi-maven-plugin` for binary compatibility monitoring. [DONE]
    - **Build Standard**: Integrated professional Maven plugins (Enforcer, JaCoCo, Source, Javadoc, PITest, JMH) and achieved ~60% engine coverage. [DONE]
    - **Determinism**: Introduced `randomSeed` in `Configuration` to ensure reproducible simulations and stable CI tests. [DONE]
    - **Thread-Safety & Lifecycle**: Refactored `GameLoop` with `volatile` dependencies, `BooleanSupplier` for zero-boxing termination checks, and polymorphic task registration to eliminate `instanceof`. [DONE]
    - **API Contract**: Enhanced Javadoc for all `@EngineAPI` components and enabled `ServiceLoader` discovery for plugins. [DONE]
    - **Performance Hot-Path**: Refactored `AnimalHealthSystem` for direct SoA storage access, bypassing object wrappers in the main simulation tick. [DONE]
    - **Developer Onboarding**: Added Quick Start guide to `README.md`. [DONE]
    - **Codebase Code Review**: Applied Java Code Reviewer suggestions across the project. Optimized `PopulationService` to eliminate Stream API allocations in hot paths, removed magic numbers, split God methods, and added `@RequiredArgsConstructor`. Fixed regression in SimCity test suite. [DONE]

- **Phase 4: Modularization & API Stabilization (May 2026)**:
    - **Engine Isolation**: Transitioned from monolith to multi-module Maven structure. Defined public API via `module-info.java`, `@EngineAPI` and `@InternalEngine` annotations. Restricted `com.island.engine.parallel` package from exports to ensure implementation encapsulation. [DONE]
    - **Engine Library Readiness**:
        - Refactored package structure to isolate implementation details in `.internal` packages. [DONE]
        - Fixed JPMS leaks by moving `ParallelTask` to exported `core` package. [DONE]
        - Implemented static factory methods in `EventBus` and `ComponentStore` to decouple domains from concrete implementations. [DONE]
        - Updated `ArchitectureTest` to enforce strictly encapsulated library boundaries. [DONE]
    - **Engine Quality & Performance**:
        - Refactored `GameLoop` for robust thread-safety and optimized `PhaseScheduler` with version-based task tracking. [DONE]
        - Optimized ECS `SystemExecutionGraph` conflict detection by avoiding `HashSet` allocations and using direct list iteration. [DONE]
        - Optimized `DefaultEventBus` hierarchy traversal using `ArrayDeque`. [DONE]
        - Optimized `EntityArchetype` querying and ECS implementation details. [DONE]
        - Enhanced `ParallelTask` flexibility with custom conflict detection. [DONE]
    - **Plugin Isolation**: Added `module-info.java` to `island-nature` and `island-simcity` with refined exports, achieving full JPMS isolation for all core modules. [DONE]
    - **Architecture Cleanup**: 
        - Deleted redundant root `src` directory and organized scripts. [DONE]
        - Moved `ArchitectureTest` to `island-app` and expanded with cross-module isolation rules and internal API usage checks. [DONE]
        - Fixed JPMS isolation: eliminated internal engine exports and replaced with public storage interfaces (`EntityIdProvider`, `HealthStorage`, `AgeStorage`). [DONE]
        - Moved `GameLoopOptimizationTest` to `island-engine`. [DONE]
        - Relocated `WorldInitializationTest` to `island-nature` (integration package) and fixed its package declaration to align with module boundaries. [DONE]
        - Refactored `DefaultWorkUnit` to use `AbstractList`. [DONE]
        - Fixed thread-safety (TOCTOU) in `GameLoop` and `CityMap`. [DONE]
        - Resolved `EconomySystem` placeholder crashes and fixed `SimulationConfig` execution mode logic. [DONE]
    - **Automation**: Added GitHub Actions CI workflow for automated build and architecture verification. [DONE]
    - **API Misuse Fixes**: Refactored `SimCitySmokeTest` and `SimCityCoreLogicTest` to use the public `SimulationEngine` API instead of manually assembling internal components like `ParallelDispatcher` or `PhaseScheduler`. [DONE]
    - **ECS Optimization**: 
        - Integrated `SystemExecutionGraph` into `PhaseScheduler` for automated parallel scheduling. [DONE]
        - Implemented 6 split ECS systems in Nature domain. [DONE]
    - **SimCity Test Alignment**: Audited SimCity boundary and smoke tests. Updated test assertions to reflect current simulation engine logic regarding happiness mechanics, infrastructure connectivity, and pollution thresholds. Validated current behavior is consistent with project requirements. [DONE]
        - **Clean Nature ECS**: Eliminated `instanceof` in `ConsumableComponent` by using specific `Cell` context. Verified `StatisticsService` uses typed events. [DONE]
        - **Utility Independence**: Verified `util/interaction` is correctly located in `nature.model`. [DONE]
        - **SimCity Expansion**: Added Agricultural zones and expanded infrastructure to include Railways, Metro, Water Supply, and Electricity. Implemented **Pollution System** and comprehensive boundary condition testing (economic, spatial, density, social, power, and pollution logic). [DONE]

- **Phase 3: Global Systems & Performance Tuning (May 2026)**:
    - **Climate System**: Implemented `ClimateService` managing seasonal transitions. [DONE]
    - **Environmental Impact**: Integrated temperature into growth and health systems. [DONE]
    - **Landscape & Terrain**: Implemented dynamic river generation as a natural movement barrier. [DONE]

## Next Steps
    - **Performance**:
        - Further optimize `SystemExecutionGraph` for large-scale simulations using more granular dependency groups.
        - Investigate native memory usage (off-heap) for high-density entity stores to minimize GC impact further.
        - Refactor `MovementComponent` to SoA storage to complete the primitive data migration.
    - **Maintenance & Technical Debt**:
        - Periodically review `ObjectPool` fragmentation under extreme stress tests.
        - Expand `EcosystemBalanceTest` with edge-case climate scenarios (volcanic winter, heat waves).
        - Improve JaCoCo coverage to reach 75% threshold (currently 60%).
        - Implement JMH benchmarks for EventBus and ECS query performance.
