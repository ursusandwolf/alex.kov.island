# Island Ecosystem Simulator: Roadmap & TODO

## 🚀 Phase 1: Architectural Foundation (Completed / In Progress)
- [x] **Vector 1: Event-Driven Architecture**
    - [x] Design and implement a lightweight `EventBus` in the engine.
    - [x] Refactor `StatisticsService` to subscribe to events instead of direct monitoring.
    - [x] Refactor `FeedingService` to publish `EntityDiedEvent`.
    - [x] Refactor `LifecycleService` to publish `EntityDiedEvent`.
    - [x] Add `LogService` or `AlertService` that reacts to specific events.
- [x] **Vector 2: ECS (Entity-Component-System) Evolution**
    - [x] Define `Component` interface and base implementation.
    - [x] Refactor `Organism` to use a hybrid component-based approach.
    - [x] **Technical Debt: Performance Optimization**
        - [x] Replace `Map<Class, Component>` with indexed array or fixed fields for hot components (Health, Age).
        - [x] Eliminate Map lookup overhead in hot simulation cycles.
    - [ ] Migrate `MovementService` to operate on `PositionComponent`.
    - [ ] Decouple `SimulationWorld` from specific entity classes.

## 🚀 Sprint 3: Advanced ECS & Performance (Active)
- [ ] **Task 1: System Execution Graph**
    - [ ] Update `EntitySystem` to declare read/write components instead of just `requiredComponents()`.
    - [ ] Implement `SystemExecutionGraph` for static dependency resolution (DAG generation).
    - [ ] Group independent systems for parallel execution.
- [ ] **Task 2: ECS Archetypes**
    - [ ] Implement `EntityArchetype` (immutable set of component classes).
    - [ ] Refactor `EntityContainer` and `Cell` to group entities logically by archetype (`Map<EntityArchetype, Set<Entity>>`).
    - [ ] Optimize `EntityQuery` to match and fetch archetypes in O(1).
- [ ] **Task 3: Final Architectural Cleanup**
    - [ ] Category 4: ConsumableComponent handles biomass consumption (remove `instanceof Biomass`).
    - [ ] Category 5: Move growth from `Biomass.grow` to `BiomassGrowthSystem`.
    - [ ] Apply node narrowing and typed event patterns to the SimCity module.
- [ ] **Task 4: Performance Benchmarking**
    - [ ] Create `SimulationBenchmarkTest` to measure TPS.
    - [ ] Establish new performance baselines for large grids.

## 🛠 Immediate Technical Debt & Bug Fixes
- [x] **Bug: Inconsistent Death Reporting**
    - [x] Unified `STARVATION` + `HUNGER` into a single `HUNGER` death cause.
- [x] **Performance: GameLoop Allocations**
    - [x] Reuse phase-based collection structures in `GameLoop.runTick()` to reduce GC pressure.
- [x] **Engine: EventBus Improvements**
    - [x] Implement hierarchical event matching (subscribe to superclasses).
    - [x] Add `unsubscribe` mechanism for dynamic component lifecycle.
- [x] **Performance: Sampling Strategy**
    - [x] Improve `SamplingUtils` to avoid $O(N)$ skip on large `LinkedHashSet` collections.

## 📈 Phase 2: Optimization & Scalability
- [ ] **Vector 3: Dynamic Load Balancing**
    - [ ] Implement `DynamicChunkingStrategy`.
    - [ ] Add monitoring for thread load per chunk.
- [ ] **Performance Tuning**
    - [ ] Profiling GC and Object Pools using `java-performance` skill.
    - [ ] Optimize `GridUtils` locking mechanisms.

## 🌍 Phase 3: Global Systems & Integration
- [ ] **Vector 4: Climate & Global Systems**
    - [ ] Create a "Global Plugin" architecture.
    - [ ] Implement `SeasonPlugin` (Weather, Temperature) affecting all other plugins.
- [ ] **Vector 5: Headless & API**
    - [ ] Decouple View from Engine via WebSockets/REST.
    - [ ] Create a basic web-dashboard for simulation monitoring.

## 🛠 Maintenance
- [ ] Increase test coverage for concurrent scenarios.
- [ ] Update `GEMINI.md` with new architectural rules.
