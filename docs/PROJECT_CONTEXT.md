# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Code Review v4 issues have been addressed (double reporting, thread-safety, EventBus robustness, GC efficiency).
- Global and project-specific `GEMINI.md` files are configured.

## Recent Changes
- **GC Pressure Reduction**: Refactored `GameLoop` to use `CellProcessor` pooling and `CountDownLatch`, eliminating object churn (`Callable`, `Future`, lambdas) in the simulation hot path. Reused task collection structures across phases.
- **Thread Safety**: Optimized component visibility by making core fields `volatile` in `HealthComponent` and `AgeComponent`, ensuring stability in parallel execution modes.
- **AlertService Refactoring**: Eliminated magic strings in event handling, aligning reporting with `DeathCause` enum constants.
- **Hierarchical EventBus**: Improved `DefaultEventBus` to support hierarchical event matching and unsubscription. Added comprehensive `EventBusTest`.
- **ECS Performance**: Optimized `Organism` by replacing Map lookups with direct field references for "hot" components (`HealthComponent`, `AgeComponent`).
- **Efficient Sampling**: Optimized `SamplingUtils` to use $O(1)$ indexed access for `RandomAccess` collections.

## Pending Items
- Continue simulation development or refactoring as requested.
