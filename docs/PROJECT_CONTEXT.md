# Project Context

## Current State
- **Architectural Cleanup (May 2026)**:
    - **ECS Refinement**: Refactored `ComponentRegistry` from a static singleton to an instance-based registry, enabling full simulation isolation. Updated `ArrayComponentStore` with dynamic growth logic to prevent silent component loss.
    - **System Splitting**: Monolithic `HealthSystem` and `MovementSystem` have been split into specialized versions: `AnimalHealthSystem`, `BiomassGrowthSystem`, `AnimalMovementSystem`, and `BiomassMovementSystem`. This eliminates OCP violations and removes `instanceof` dispatching.
    - **Typed Events**: Introduced `AnimalBornEvent` and `AnimalDiedEvent` to provide type-safe lifecycle notifications, removing the need for broad `instanceof` checks in `StatisticsService` and `AlertService`.
    - **API Stabilization**: Simplified the `EntitySystem` contract by removing redundant `process(T, int)` signatures that were incompatible with the parallel `CellService` path.
    - **Build & Test Stability**: Mass-updated the test suite to align with new constructor injections and system names. Resolved all compiler and syntax issues in core classes (`Island`, `WorldInitializer`). Updated all relevant test files to use `AnimalFeedingSystem` and `AnimalReproductionSystem`.

## Technical Debt / Known Issues
- **ECS Transition**: `FeedingService` and `ReproductionService` still exist but tests have been migrated to the new `AnimalFeedingSystem` and `AnimalReproductionSystem`. The old service classes should be removed once the migration is fully verified.
- **Component Data**: `GrowthComponent` and `MetabolismComponent` are still mostly marker components; biological parameters should be moved from `Configuration` into these components for a purer ECS approach.
- **Checkstyle**: There are still some Checkstyle violations that need to be addressed to achieve a clean build without `-Dcheckstyle.skip`.

## Pending Items
- Finalize the migration of all services to a fully component-based ECS approach (Sprint 2 tasks).
- Implement `ComponentFactory` for standard component bundles (S2-3).
- Implement "Consumable" component for `FeedingService` to remove remaining `instanceof` checks during predation.
