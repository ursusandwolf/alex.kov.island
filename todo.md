# Island Ecosystem Simulator: Roadmap & TODO

## 🚀 Phase 1: Architectural Foundation (Completed / In Progress)
- [x] **Vector 1: Event-Driven Architecture**
    - [x] Design and implement a lightweight `EventBus` in the engine.
    - [x] Refactor `StatisticsService` to subscribe to events instead of direct monitoring.
    - [x] Refactor `FeedingService` to publish `EntityDiedEvent`.
    - [x] Refactor `LifecycleService` to publish `EntityDiedEvent`.
    - [ ] Add `LogService` or `AlertService` that reacts to specific events.
- [x] **Vector 2: ECS (Entity-Component-System) Evolution**
    - [x] Define `Component` interface and base implementation.
    - [x] Refactor `Organism` to use a hybrid component-based approach.
    - [ ] **Technical Debt: Performance Optimization**
        - [ ] Replace `Map<Class, Component>` with indexed array or fixed fields for hot components (Health, Age).
        - [ ] Eliminate Map lookup overhead in hot simulation cycles.
    - [ ] Migrate `MovementService` to operate on `PositionComponent`.
    - [ ] Decouple `SimulationWorld` from specific entity classes.

## 🛠 Immediate Technical Debt & Bug Fixes
- [x] **Bug: Inconsistent Death Reporting**
    - [x] Unified `STARVATION` + `HUNGER` into a single `HUNGER` death cause.
- [ ] **Performance: GameLoop Allocations**
    - [ ] Reuse phase-based collection structures in `GameLoop.runTick()` to reduce GC pressure.
- [ ] **Engine: EventBus Improvements**
    - [ ] Implement hierarchical event matching (subscribe to superclasses).
    - [ ] Add `unsubscribe` mechanism for dynamic component lifecycle.
- [ ] **Performance: Sampling Strategy**
    - [ ] Improve `SamplingUtils` to avoid $O(N)$ skip on large `LinkedHashSet` collections.

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
