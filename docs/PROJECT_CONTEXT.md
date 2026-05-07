# Project Context

## Current State
- **Sprint 3: Advanced ECS & Performance Completed (May 2026)**:
    - **ECS Archetypes**: Implemented `EntityArchetype` and refactored `EntityContainer` for $O(1)$ group iteration.
    - **Architectural Cleanup**: Biomass consumption fully migrated to `ConsumableComponent`. Growth logic moved to `BiomassGrowthSystem`.
    - **SimCity Alignment**: Aligned SimCity module with engine patterns (typed events, node narrowing base class).
    - **Performance Optimization**: System execution graph with static dependency resolution and parallel grouping.

## Next Steps: Phase 2 - Optimization & Scalability
- **Dynamic Load Balancing**: Implement `DynamicChunkingStrategy` to handle non-uniform entity distribution.
- **Extreme Scale Profiling**: Utilize `java-performance` to optimize object pools and locking overhead for grids larger than 100x100.
- **DI Refactoring**: Finalize cleanup of manual dependency assembly in `NaturePlugin`.

## Maintenance & Technical Debt
    - **Validation**: Review `shouldStop` semantics (all animals vs individual species).
    - **Test Coverage**: Add stress tests for concurrent archetype transitions.
