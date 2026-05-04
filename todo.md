# Island Ecosystem Simulator: Roadmap & TODO

## 🚀 Phase 1: Architectural Foundation (Current)
- [ ] **Vector 1: Event-Driven Architecture**
    - [ ] Design and implement a lightweight `EventBus` in the engine.
    - [ ] Refactor `FeedingService` to publish `EntityDiedEvent`.
    - [ ] Refactor `StatisticsService` to subscribe to events instead of direct monitoring.
    - [ ] Add `LogService` or `AlertService` that reacts to specific events.
- [ ] **Vector 2: ECS (Entity-Component-System) Evolution**
    - [ ] Define `Component` interface and base implementation.
    - [ ] Refactor `Organism` to use a component-based approach (extract `Position`, `Health`, `Hunger` into components).
    - [ ] Migrate `MovementService` to operate on `PositionComponent`.
    - [ ] Decouple `SimulationWorld` from specific entity classes.

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
