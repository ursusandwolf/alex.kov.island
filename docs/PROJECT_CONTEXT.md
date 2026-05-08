# Project Context

## Current State
- **Phase 4: Modularization (May 2026)**:
    - **Engine Isolation**: Transitioned from monolith to multi-module Maven structure. Defined public API via `module-info.java`, `@EngineAPI` and `@InternalEngine` annotations. [DONE]
    - **Architecture Cleanup**: 
        - Deleted redundant root `src` directory and organized scripts. [DONE]
        - Moved `ArchitectureTest` to `island-app` and expanded with cross-module isolation rules. [DONE]
        - Moved `GameLoopOptimizationTest` to `island-engine`. [DONE]
        - Refactored `DefaultWorkUnit` to use `AbstractList`. [DONE]
        - Fixed thread-safety (TOCTOU) in `GameLoop` and `CityMap`. [DONE]
    - **ECS Optimization**: 
        - Integrated `SystemExecutionGraph` into `PhaseScheduler` for automated parallel scheduling. [DONE]
        - Implemented 6 split ECS systems in Nature domain. [DONE]
    - **Refactoring & Type Safety**:
        - **SimCity ECS**: Migrated SimCity to a pure ECS architecture, removing `Resident` and `Building` subclasses. Fixed logic and thread-safety in tests. [DONE]
        - **Clean Nature ECS**: Eliminated `instanceof` in `ConsumableComponent` by using specific `Cell` context. Verified `StatisticsService` uses typed events. [DONE]
        - **Utility Independence**: Verified `util/interaction` is correctly located in `nature.model`. [DONE]

- **Phase 3: Global Systems & Performance Tuning (May 2026)**:
    - **Climate System**: Implemented `ClimateService` managing seasonal transitions. [DONE]
    - **Environmental Impact**: Integrated temperature into growth and health systems. [DONE]
    - **Landscape & Terrain**: Implemented dynamic river generation as a natural movement barrier. [DONE]

## Next Steps
- **Genetic Evolution**: 
    - **Trait System**: Define a set of inheritable traits (speed, vision range, metabolism efficiency, temperature resistance).
    - **Mutation & Crossover**: Implement logic for trait inheritance with mutation during reproduction.
    - **Natural Selection**: Allow environmental factors (climate, food availability) to drive evolution via trait-based survival.
- **Performance**:
    - Further optimize `SystemExecutionGraph` for large-scale simulations.
    - Investigate native memory usage for high-density entity stores.

## Maintenance & Technical Debt
- **Validation**: Continuous monitoring of ecosystem stability across seasonal extremes.
- **Test Coverage**: Optimized suite to 75 high-signal tests (consolidated from 82); maintained 100% pass rate. [DONE]
- **Clean Code**: Periodic review of generic type usage to ensure absolute type safety across domain boundaries.
