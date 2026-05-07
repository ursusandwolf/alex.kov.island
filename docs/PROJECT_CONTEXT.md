# Project Context

## Current State
- **ECS Migration (Sprint 2) Completed (May 2026)**:
    - **Feeding & Reproduction**: Migrated legacy services to `AnimalFeedingSystem` and `AnimalReproductionSystem`.
    - **Component-Based Logic**: Introduced `ConsumableComponent` and `ReproductionComponent` to remove `instanceof` checks and move parameters to ECS layer.
    - **Centralized Initialization**: Implemented `NatureComponentFactory` used by all nature entities.
    - **Test Robustness**: Migrated test suite to the new architecture and added extinction-based stop condition tests.
- **Architectural Cleanup**:
    - **ECS Refinement**: Refactored `ComponentRegistry` from a static singleton to an instance-based registry, enabling full simulation isolation. Updated `ArrayComponentStore` with growth logic.
    - **System Splitting**: `HealthSystem` and `MovementSystem` split into `AnimalHealthSystem`, `BiomassGrowthSystem`, `AnimalMovementSystem`, and `BiomassMovementSystem`.
    - **Typed Events**: Introduced `AnimalBornEvent` and `AnimalDiedEvent` to the `EventBus`, removing `instanceof` in `StatisticsService` and `AlertService`.
    - **API Stabilization**: Simplified `EntitySystem` contract by removing the dead `process(T, int)` method.

## Pending Items
- **Sprint 3: Advanced ECS & Performance**:
    - Implement `SystemExecutionGraph` for automatic task ordering and parallel execution optimization.
    - Introduce `Archetypes` for even faster entity creation and iteration.
- **Maintenance**:
    - Refactor remaining `Biomass.grow` and `consumeBiomass` to a fully component-based ECS approach (Sprint 2 tasks).
    - Implement `ComponentFactory` for standard component bundles (S2-3). [DONE]
    - Implement "Consumable" component for `FeedingService` to remove remaining `instanceof` checks during predation. [DONE]
