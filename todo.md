# Island Ecosystem Simulator: Roadmap & TODO

## 🚀 Phase 1: Architectural Foundation (Completed)
- [x] **Vector 1: Event-Driven Architecture**
- [x] **Vector 2: ECS (Entity-Component-System) Evolution**

## 🚀 Sprint 3: Advanced ECS & Performance (Completed)
- [x] **Task 1: System Execution Graph**
    - [x] Update `EntitySystem` to declare read/write components.
    - [x] Implement `SystemExecutionGraph` for static dependency resolution.
    - [x] Group independent systems for parallel execution.
- [x] **Task 2: ECS Archetypes**
    - [x] Implement `EntityArchetype` (immutable set of component classes).
    - [x] Refactor `EntityContainer` and `Cell` to group entities logically by archetype.
    - [x] Optimize `EntityQuery` to match and fetch archetypes in O(1).
- [x] **Task 3: Final Architectural Cleanup**
    - [x] Category 4: ConsumableComponent handles biomass consumption (remove `instanceof Biomass` in `AnimalFeedingSystem`).
    - [x] Category 5: Move growth logic fully from `Biomass.grow` to `BiomassGrowthSystem`.
    - [x] Apply node narrowing and typed event patterns to the SimCity module.
- [x] **Task 4: Performance Benchmarking & GC Optimization**

## 🛠 Immediate Technical Debt & Bug Fixes
- [x] **Technical Debt: NaturePlugin Assembly**
    - [x] Refactor `NaturePlugin` constructor to use a cleaner DI-like approach or Factory for domain context assembly.
- [x] **Code Quality: Dead Code Removal**
    - [x] Remove empty/unused `process()` overrides in ECS systems.
- [x] **Architecture: Test Organization**
- [x] **Engine: EventBus Improvements**

## 📈 Phase 2: Optimization & Scalability
- [x] **Vector 3: Dynamic Load Balancing**
    - [x] Implement `DynamicChunkingStrategy`.
    - [x] Add monitoring for thread load per chunk.
- [ ] **Performance Tuning**
    - [ ] Profiling GC and Object Pools using `java-performance` skill.
    - [ ] Optimize `GridUtils` locking mechanisms.

## 🌍 Phase 3: Global Systems & Integration
- [ ] **Vector 4: Climate & Global Systems**
- [ ] **Vector 5: Headless & API**

## 🛠 Maintenance
- [ ] Increase test coverage for concurrent scenarios.
- [ ] Update `GEMINI.md` with new architectural rules.
- [x] Review shouldStop semantic change.
