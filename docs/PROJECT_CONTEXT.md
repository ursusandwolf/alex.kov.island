# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Code Review v5 issues have been addressed (GameLoop refactoring, abstraction leaks, lifecycle fixes, event mechanism unification).
- The engine is now decoupled from domain-specific stop conditions and configurations.
- Parallel processing is handled by dedicated dispatcher and scheduler.
- Global and project-specific `GEMINI.md` guidelines are being followed.

## Recent Changes
- **GameLoop Decomposition**: Extracted `PhaseScheduler` and `ParallelDispatcher` from `GameLoop` to address the God Class issue.
- **Abstraction Integrity**: Removed the configuration generic parameter from `SimulationWorld` and fixed abstraction leaks in `NatureLauncher`.
- **Lifecycle Management**: Implemented proper `stop()` in `SimulationEngine` and added `shouldStop` and `onSimulationStopped` hooks to `SimulationPlugin`.
- **Event Mechanism Unification**: Removed `WorldListener` interface in favor of direct world notification methods that publish to `EventBus`.
- **Thread Safety**: Fixed potential data races in `CellProcessor` by marking fields as `volatile`.
- **Robustness**: Refactored `DefaultEventBus` type resolution to be iterative and protected it from subscriber exceptions.
- **Configurability**: Moved monitoring magic numbers to `Configuration`.
- **Log Consistency**: Unified log messages in `NatureLauncher` to English.

## Pending Items
- Continue simulation development or refactoring as requested.
- Explicitly decide on ECS direction (currently in mixed state for performance).
