# Project Context

## Current State
- **Phase 3: Global Systems & Performance Tuning (May 2026)**:
    - **Climate System**: Implemented `ClimateService` managing seasonal transitions and granular temperature fluctuations.
    - **Environmental Impact**: Integrated temperature into `BiomassGrowthSystem` (heat/cold stress) and `AnimalHealthSystem` (metabolism scaling for warm/cold-blooded).
    - **Memory Optimization**: Refactored `EntityContainer` to use memory-efficient `ArrayList` buckets and removed redundant views, significantly reducing footprint for millions of entities.
    - **Engine Efficiency**: Optimized `ArrayComponentStore` with lazy initialization and reduced locking overhead in `Organism` by relying on phase-based execution guarantees.
    - **Initialization Speed**: Introduced "silent" entity addition during world setup to bypass event bus overhead, preventing OOM during large-scale population.

- **Phase 2: Optimization & Scalability Completed (May 2026)**:
    - **Dynamic Load Balancing**: Implemented `DynamicChunkingStrategy` with recursive splitting based on entity density. Added `WorkUnit` abstraction in the engine for performance telemetry.
    - **Performance Monitoring**: `ParallelDispatcher` tracks and records execution time per `WorkUnit`, enabling data-driven rebalancing.
    - **DI Refactoring**: Completed cleanup of manual dependency assembly in `NaturePlugin` by extracting `NatureDomainContextFactory`.

## Next Steps
- **Headless Mode**: Decouple simulation engine from ConsoleView for CI/CD and automated benchmarking.
- **Genetic Evolution**: Introduce mutation and trait inheritance for animals to adapt to climate changes.
- **Spatial Interactions**: Implement inter-cell dependencies (e.g., forest fires, water flow).

## Maintenance & Technical Debt
    - **Validation**: Continuous monitoring of ecosystem stability across seasonal extremes.
    - **Test Coverage**: Maintain `ExtremeScalePerformanceTest` as a regression gate for memory regressions.
