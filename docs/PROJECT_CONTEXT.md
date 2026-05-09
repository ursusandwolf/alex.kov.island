# Project Context

## Current State
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
        - Moved `GameLoopOptimizationTest` to `island-engine`. [DONE]
        - Relocated `WorldInitializationTest` to `island-nature` (integration package) and fixed its package declaration to align with module boundaries. [DONE]
        - Refactored `DefaultWorkUnit` to use `AbstractList`. [DONE]
        - Fixed thread-safety (TOCTOU) in `GameLoop` and `CityMap`. [DONE]
    - **API Misuse Fixes**: Refactored `SimCitySmokeTest` and `SimCityCoreLogicTest` to use the public `SimulationEngine` API instead of manually assembling internal components like `ParallelDispatcher` or `PhaseScheduler`. [DONE]
    - **ECS Optimization**: 
        - Integrated `SystemExecutionGraph` into `PhaseScheduler` for automated parallel scheduling. [DONE]
        - Implemented 6 split ECS systems in Nature domain. [DONE]
    - **Refactoring & Type Safety**:
        - **SimCity ECS**: Migrated SimCity to a pure ECS architecture, removing `Resident` and `Building` subclasses. Fixed logic and thread-safety in tests. [DONE]
        - **Clean Nature ECS**: Eliminated `instanceof` in `ConsumableComponent` by using specific `Cell` context. Verified `StatisticsService` uses typed events. [DONE]
        - **Utility Independence**: Verified `util/interaction` is correctly located in `nature.model`. [DONE]
        - **SimCity Expansion**: Added Agricultural zones and expanded infrastructure to include Railways, Metro, Water Supply, and Electricity. Implemented **Pollution System** and comprehensive boundary condition testing (economic, spatial, density, social, power, and pollution logic). [DONE]

- **Phase 3: Global Systems & Performance Tuning (May 2026)**:
    - **Climate System**: Implemented `ClimateService` managing seasonal transitions. [DONE]
    - **Environmental Impact**: Integrated temperature into growth and health systems. [DONE]
    - **Landscape & Terrain**: Implemented dynamic river generation as a natural movement barrier. [DONE]

## Next Steps
    - **Performance**:
        - Optimized `GridUtils` locking mechanism [DONE].
        - Established foundation for Structure of Arrays (SoA) memory optimization:
            - Implemented `EntityIdManager` for unique entity tracking.
            - Created `HealthSoAStore` and `AgeSoAStore` for high-density primitive storage.
            - Integrated SoA stores into `NatureDomainContext`. [DONE]
        - Next: Migrate `Organism` logic to use SoA stores for component data.
        - Further optimize `SystemExecutionGraph` for large-scale simulations.
        - Reviewed `ObjectPool` usage in `AnimalFactory` and `Organism` poolable implementation; current implementation is performant and thread-safe. [DONE]    - Investigate native memory usage for high-density entity stores.

## Maintenance & Technical Debt
- **Validation**: Continuous monitoring of ecosystem stability across seasonal extremes.
- **Test Coverage**: Optimized suite to 75 high-signal tests (consolidated from 82); maintained 100% pass rate. [DONE]
- **Clean Code**: Periodic review of generic type usage to ensure absolute type safety across domain boundaries.
