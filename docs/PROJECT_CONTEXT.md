# Project Context

## Current State
- **Phase 4: Modularization (May 2026)**:
    - **Engine Isolation**: Transitioning from monolith to multi-module Maven structure (island-engine, island-nature, island-simcity). [IN PROGRESS]
    - **Architecture Cleanup**: 
        - Moved `InteractionMatrix` and `InteractionProvider` to `nature.model` to free `util` from domain dependencies. [DONE]
        - Removed redundant `EntityBornEvent`/`EntityDiedEvent` from engine. [DONE]
    - **ECS Optimization**: 
        - Integrated `SystemExecutionGraph` into `PhaseScheduler` for automated parallel scheduling. [DONE]
        - Implemented 6 split ECS systems in Nature domain. [DONE]
    - **Refactoring (May 2026)**:
        - **SimCity ECS**: Migrated SimCity to a pure ECS architecture, removing `Resident` and `Building` subclasses and using components (`PopulationComponent`, `BuildingComponent`, `EconomyComponent`). [DONE]
        - **Type Safety**: Refactored `ConsumableComponent<T>` with `ConsumeAction<T>` to eliminate hidden `instanceof` checks in consumption logic. [DONE]

- **Phase 3: Global Systems & Performance Tuning (May 2026)**:
    - **Climate System**: Implemented `ClimateService` managing seasonal transitions. [DONE]
    - **Environmental Impact**: Integrated temperature into growth and health systems. [DONE]
    - **Landscape & Terrain**: Implemented dynamic river generation as a natural movement barrier. [DONE]

## Next Steps
- **Modularization**: 
    - Execute Maven multi-module migration (split `pom.xml`).
    - Define public API via `module-info.java` and `@EngineAPI` annotations.
- **Genetic Evolution**: 
    - Introduce mutation and trait inheritance for animals to adapt to climate changes.
    - Implement trait crossover for animal reproduction.

## Maintenance & Technical Debt
- **Validation**: Continuous monitoring of ecosystem stability across seasonal extremes.
- **Test Coverage**: Optimized suite to 75 high-signal tests (consolidated from 82); maintained 100% pass rate. [DONE]
- **Clean Code**: Periodic review of generic type usage to ensure absolute type safety across domain boundaries.
