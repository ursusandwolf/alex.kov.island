# Project Context

## Current State
- **Major Refactoring (May 2026)**: 
    - Reorganized `com.island.nature.entities`, `com.island.engine`, and `com.island.util` into specialized sub-packages.
    - **ECS Implementation**: Introduced `ComponentStore` (Default and Array implementations) and `EntityQuery`.
    - **System Migration**: Migrated `LifecycleService` to `HealthSystem` and `MovementService` to `MovementSystem` using the ECS System pattern.
    - **Service Cleanup**: Removed deprecated `LifecycleService` and `MovementService`.
    - **Build Stability**: Fixed numerous test compilation errors related to syntax and missing imports.

## Technical Debt / Known Issues
- **ECS Transition**: `FeedingService` and `ReproductionService` still follow the old service pattern and should be migrated to `EntitySystem` in the next sprint.
- **Checkstyle**: There are still some Checkstyle violations that need to be addressed to achieve a clean build without `-Dcheckstyle.skip`.

## Pending Items
- Finalize the migration of all services to a fully component-based ECS approach (Sprint 2 tasks).
- Implement `ComponentFactory` for standard component bundles (S2-3).
- Update UML diagrams to reflect the new ECS architecture and package structure.
