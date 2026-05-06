# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Critical technical debt from v5/v6 has been resolved:
    - `SpeciesKey` singleton replaced with registry-based management.
    - `PhaseScheduler` and `ParallelDispatcher` thread-safety and resource management improved.
    - `SimulationContext` modernized as a Java Record.
- Centralized event publication (HF-1) implemented: all deaths are now reported via `Island.onEntityRemoved`, eliminating double-counting.
- Engine is fully decoupled from domain-specific logic.

## Recent Changes
- **Species Registry Cleanup**: Removed redundant `SpeciesKey` and optimized `SpeciesLoader`.
- **Herbivore Lifecycle**: Modernized `Butterfly` and `Caterpillar` lifecycle management.
- **ParallelTask Abstraction**: Decoupled engine scheduling from domain-specific `CellService` using a new `ParallelTask` interface and `asParallelTask()` visitor-like method.
- **SpeciesRegistry Modernization**: Optimized registry with `Map.copyOf` and cached species codes for O(1) code set retrieval.
- **Improved Test Coverage**: Integrated `SimulationStopConditionTest` and `StatisticsDeathCountingTest`.

## Pending Items
- Enhance ECS system layer for more complex logic.
- Consider adding a `System` layer to handle domain logic outside of `CellService`.
- Implement more complex behaviors in SimCity plugin to test the new parallel abstraction.
