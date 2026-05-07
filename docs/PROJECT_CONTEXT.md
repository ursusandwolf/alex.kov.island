# Project Context

## Current State
- **Phase 2: Optimization & Scalability In Progress (May 2026)**:
    - **Dynamic Load Balancing**: Implemented `DynamicChunkingStrategy` with recursive splitting based on entity density. Added `WorkUnit` abstraction in the engine for performance telemetry.
    - **Performance Monitoring**: `ParallelDispatcher` now tracks and records execution time per `WorkUnit`, enabling data-driven rebalancing.
    - **Architectural Integrity**: Decoupled engine from domain models via `WorkUnit` interface, maintaining strict package boundaries.
    - **DI Refactoring**: Completed cleanup of manual dependency assembly in `NaturePlugin` by extracting `NatureDomainContextFactory`.

- **Sprint 3: Advanced ECS & Performance Completed (May 2026)**:
    - **ECS Archetypes**: Implemented `EntityArchetype` and refactored `EntityContainer` for $O(1)$ group iteration.
    - **Architectural Cleanup**: Biomass consumption fully migrated to `ConsumableComponent`. Growth logic moved to `BiomassGrowthSystem`.
    - **SimCity Alignment**: Aligned SimCity module with engine patterns (typed events, node narrowing base class).
    - **Performance Optimization**: System execution graph with static dependency resolution and parallel grouping.

## Next Steps
- **Extreme Scale Profiling**: Utilize `java-performance` to optimize object pools and locking overhead for grids larger than 100x100.
- **Headless Mode**: Decouple simulation engine from ConsoleView for CI/CD and benchmarking.
- **Climate Systems**: Introduce seasonal and environmental modifiers to entity behavior.

## Maintenance & Technical Debt
    - **Validation**: Completed review of `shouldStop` semantics; implementation correctly triggers on total animal extinction (excluding biomass).
    - **Test Coverage**: Add stress tests for concurrent archetype transitions.
