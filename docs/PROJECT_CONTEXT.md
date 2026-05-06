# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Critical technical debt from v5/v6 has been resolved:
    - `SpeciesKey` singleton replaced with registry-based management.
    - `PhaseScheduler` and `ParallelDispatcher` thread-safety and resource management improved.
    - `SimulationContext` modernized as a Java Record.
- Double-counting of entity deaths remains resolved via centralized event publication.
- Engine is fully decoupled from domain-specific logic.

## Recent Changes
- **SpeciesKey Refactoring**: Eliminated global state by moving the key registry from `SpeciesKey` to `SpeciesRegistry`.
- **Scheduler Thread-Safety**: Ensured `PhaseScheduler` is safe for concurrent use by eliminating instance-level state during execution.
- **Pool Management**: Added dynamic shrinking to the `ParallelDispatcher` processor pool.
- **Boilerplate Reduction**: Refactored `SimulationContext` to a record.

## Pending Items
- Enhance ECS system layer for more complex logic.
- Decide on further simulation development or refactoring as requested.
