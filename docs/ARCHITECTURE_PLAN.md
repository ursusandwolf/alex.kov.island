# Modular Simulation Architecture Plan

## 1. Goal
Decouple the simulation engine from the specific "Island" domain. Enable the engine to run any simulation (e.g., traffic, city, abstract particles) by implementing a plugin.

## 2. Module Structure

### Module A: `island-core` (The Engine)
- **Generic Primitives**:
    - `SimulationWorld<T extends Mortal>`: Manage the grid/spatial structure.
    - `SimulationNode<T extends Mortal>`: Container for entities.
    - `GameLoop`: Task scheduling and execution.
    - `SimulationContext`: Global state and engine-level services.
- **Zero dependencies** on `content` (Animals, Plants, etc.).

### Module B: `island-nature` (The Island Plugin)
- **Domain Entities**: `Organism`, `Animal`, `Plant`.
- **Domain Logic**: `FeedingService`, `ReproductionService`, `MovementService`.
- **Implementation**: `Island implements SimulationWorld<Organism>`.
- **Bootstrap**: Registers nature-specific tasks into the `GameLoop`.

### Module C: `island-app` (The Launcher)
- Configuration, Main entry point, UI/View binding.

## 3. Key Changes (Roadmap)

### Phase 1: Engine Sanitization (DIP)
- [ ] Parametrize `SimulationWorld` and `SimulationNode` with `<T extends Mortal>`.
- [ ] Move animal-specific methods (`moveAnimal`, `forEachAnimal`) out of core interfaces or generalize them.
- [ ] Extract `SpeciesKey`, `SpeciesRegistry`, and `Season` to the nature plugin.

### Phase 2: Task Registration Inversion
- [ ] Remove `TaskRegistry` from core.
- [ ] Introduce a `SimulationPlugin` interface or similar mechanism to allow the nature plugin to register its own `Tickable` services.

### Phase 3: Content Isolation
- [ ] Move everything in `com.island.content` and `com.island.service` to a separate package/module.
- [ ] Ensure `com.island.engine` has NO imports from these packages.

