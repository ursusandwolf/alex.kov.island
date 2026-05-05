# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Code Review v4 issues have been addressed (double reporting, thread-safety, EventBus robustness).
- Global and project-specific `GEMINI.md` files are configured.

## Recent Changes
- **Hierarchical EventBus**: Improved `DefaultEventBus` to support hierarchical event matching and unsubscription. Added comprehensive `EventBusTest`.
- **ECS Performance**: Optimized `Organism` by replacing Map lookups with direct field references for "hot" components (`HealthComponent`, `AgeComponent`).
- **GC Pressure Reduction**: Refactored `GameLoop` to reuse phase-based collection structures, significantly reducing object allocations per tick.
- **Efficient Sampling**: Optimized `SamplingUtils` to use $O(1)$ indexed access for `RandomAccess` collections.
- **AlertService**: Added a reactive service for monitoring and logging significant simulation events via the `EventBus`.
- Fixed various technical debt items and unified death causes.

## Pending Items
- Continue simulation development or refactoring as requested.
