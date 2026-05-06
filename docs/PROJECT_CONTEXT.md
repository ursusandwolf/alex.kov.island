# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Code Review v5 issues have been addressed (GameLoop refactoring, abstraction leaks, lifecycle fixes, event mechanism unification).
- The engine is now decoupled from domain-specific stop conditions and configurations.
- Parallel processing is handled by dedicated dispatcher and scheduler.
- Global and project-specific `GEMINI.md` guidelines are being followed.

## Recent Changes
- **Single Source of Truth for Deaths**: Resolved double-counting of entity deaths by centralizing `EntityDiedEvent` publication in `Island.onEntityRemoved`. Redundant publications in `FeedingService`, `MovementService`, and `ReproductionService` were removed.
- **Race Condition Prevention**: Refactored `PhaseScheduler` to move shared state into local variables within the execution method, ensuring thread-safe scheduling even if called from multiple threads.
- **Improved Observability**: Added Javadoc for `EventBus` to expose the type hierarchy subscription feature.
- **Dispatcher Optimization**: Verified `CellProcessor` pooling in `ParallelDispatcher` through enhanced unit tests using reflection.
- **Lombok & Boilerplate Reduction**: Applied Lombok annotations (`@Slf4j`, `@Getter`, `@RequiredArgsConstructor`) across the core engine.

## Pending Items
- Address `SpeciesKey` singleton technical debt (move to registry object).
- Enhance ECS system layer for more complex logic.
- Decide on further simulation development or refactoring as requested.
