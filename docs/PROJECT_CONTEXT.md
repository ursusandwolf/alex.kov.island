# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Code Review v5 issues have been addressed (GameLoop refactoring, abstraction leaks, lifecycle fixes, event mechanism unification).
- The engine is now decoupled from domain-specific stop conditions and configurations.
- Parallel processing is handled by dedicated dispatcher and scheduler.
- Global and project-specific `GEMINI.md` guidelines are being followed.

## Recent Changes
- **Lombok & Boilerplate Reduction**: Applied Lombok annotations (`@Slf4j`, `@Getter`, `@RequiredArgsConstructor`) across the core engine (`GameLoop`, `ParallelDispatcher`, `PhaseScheduler`), significantly improving signal-to-noise ratio.
- **Dependency Injection & DIP**: Refactored `GameLoop` to use constructor injection for its components, decoupling it from specific implementations and improving testability.
- **High-Precision Timing**: Switched to `System.nanoTime()` in the simulation loop to ensure deterministic tick durations and better performance monitoring.
- **Robust Parallelism**: Enhanced `ParallelDispatcher` with synchronous fallback to prevent simulation stalls when thread pools are saturated.
- **GameLoop Decomposition**: Extracted `PhaseScheduler` and `ParallelDispatcher` from `GameLoop` to address the God Class issue.
- **Test Alignment**: Mass-updated the test suite to comply with the new engine architecture and constructor contracts.

## Pending Items
- Continue simulation development or refactoring as requested.
- Explicitly decide on ECS direction (currently in mixed state for performance).
