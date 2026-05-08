# Project Context

## Current State
- **Phase 4: Modularization (May 2026)**:
    - **Engine Isolation**: Transitioning from monolith to multi-module Maven structure (island-engine, island-nature, island-simcity).
    - **Architecture Cleanup**: Removed deprecated docs, centralized review notes, and cleaned up interaction utility dependencies.
    - **ECS Optimization**: Integrated `SystemExecutionGraph` for automated parallel scheduling based on read/write component sets.

- **Phase 3: Global Systems & Performance Tuning (May 2026)**:
    - **Climate System**: Implemented `ClimateService` managing seasonal transitions and granular temperature fluctuations.
    - **Environmental Impact**: Integrated temperature into `BiomassGrowthSystem` and `AnimalHealthSystem`.
    - **Memory Optimization**: Refactored `EntityContainer` for millions of entities.
    - **Locking Robustness**: Implemented robust read/write locking to prevent deadlocks.

## Next Steps
- **Modularization**: Complete Maven multi-module migration and move `util/interaction` to `nature` plugin.
- **Genetic Evolution**: Introduce mutation and trait inheritance for animals to adapt to climate changes.
- **Genetic Algorithms**: Implement trait crossover for animal reproduction.

## Maintenance & Technical Debt
    - **Validation**: Continuous monitoring of ecosystem stability across seasonal extremes.
    - **Test Coverage**: Maintain `ExtremeScalePerformanceTest` as a regression gate.
    - **API Stability**: Define public API via `module-info.java` and `@EngineAPI` annotations.
