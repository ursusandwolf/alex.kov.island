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
    - **Node Narrowing Cleanup**: Centralized `instanceof Cell` checks in `AbstractService`. Introduced `NatureWorld.getCell` and `moveOrganism` for type-safe domain operations. Refactored `Biomass` and `SwarmOrganism` to use `Cell` directly.

## Sprint 3: Advanced ECS & Performance (Active)
- **Task 1: System Execution Graph**
    - Develop `SystemExecutionGraph` using **Static Dependency Resolution** to manage dependencies between `EntitySystem` instances.
    - Update `EntitySystem` to replace `requiredComponents` with explicit `readComponents` and `writeComponents`.
    - Group independent systems for parallel execution using `ParallelDispatcher`.
- **Task 2: ECS Archetypes**
    - Introduce `EntityArchetype` to group entities with identical component compositions.
    - Implement **Logical Grouping** by refactoring `EntityContainer` and `Cell` to use `Map<EntityArchetype, Set<Entity>>` for $O(1)$ iteration over compatible entities.
- **Task 3: Final Architectural Cleanup**
    - **Category 4 Cleanup**: Enhance `ConsumableComponent` logic to eliminate `instanceof Biomass` checks in `AnimalFeedingSystem`.
    - **Category 5 Cleanup**: Move biomass growth logic fully from `Biomass.grow` to `BiomassGrowthSystem.process()`.
    - **SimCity Alignment**: Apply node narrowing and typed event patterns to the `SimCity` module for consistency.
- **Task 4: Performance Benchmarking & GC Optimization**
    - **Schedule Caching**: Implemented task schedule caching in `PhaseScheduler` to eliminate redundant `SystemExecutionGraph` traversals.
    - **Allocation Reduction**: Optimized `SystemExecutionGraph.buildSchedule` to avoid `HashSet` and intermediate `List` creations during conflict detection, significantly reducing per-tick GC pressure.
    - **Safe Lifecycle Management**: Fixed a self-join bug in `GameLoop.stop()` and unified simulation termination logic, ensuring `onSimulationStopped` is called reliably.
    - **Config-Driven Partitioning**: Moved remaining magic numbers in `Island` chunking logic to `Configuration`.

## Maintenance (Ongoing)
    - Refactor remaining `Biomass.grow` and `consumeBiomass` to a fully component-based ECS approach (Sprint 2 tasks).
    - Implement `ComponentFactory` for standard component bundles (S2-3). [DONE]
    - Implement "Consumable" component for `FeedingService` to remove remaining `instanceof` checks during predation. [DONE]
    - Stabilize `SimCity` module by applying similar `instanceof` narrowing refactorings.
    - GC-Optimization of hot paths in ECS iteration. [IN PROGRESS]
