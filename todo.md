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

## 🛠 Code Review Fixes (May 2026)
- [x] **Engine: Concurrency Modernization**
    - [x] Refactor `ParallelDispatcher` to use `Callable` and `ExecutorService.invokeAll()` instead of `CountDownLatch`.
    - [x] Update `GameLoop` to use `taskExecutor.submit()` instead of `new Thread()`.
- [x] **Code Quality: Checkstyle & Best Practices**
    - [x] Resolve Checkstyle violations (662 errors) or adjust `checkstyle.xml` to match project conventions.
    - [x] Mark internal engine classes (e.g., `EntityIdManager`, `PhaseScheduler`) as `final` where applicable.

## 📈 Phase 2: Optimization & Scalability
- [x] **Vector 3: Dynamic Load Balancing**
    - [x] Implement `DynamicChunkingStrategy`.
    - [x] Add monitoring for thread load per chunk.
- [x] **Performance Tuning**
    - [x] Profiling GC and Object Pools using `java-performance` skill.
    - [x] Optimize `GridUtils` locking mechanisms.

## 🌍 Phase 3: Global Systems & Integration
- [x] **Vector 4: Climate & Global Systems**
    - [x] Implement `ClimateService`.
    - [x] Add river generation as a natural movement barrier.
- [x] **Vector 5: Headless & API** (Supported by current Engine API)

## 🛠 Maintenance
- [x] Increase test coverage for concurrent scenarios.
- [x] Update `GEMINI.md` with new architectural rules.
- [x] Review shouldStop semantic change.

## 🖥 Phase 4: User Interface, Controls & Persistence (Completed)
- [x] **Vector 6: Visualization & App Layer**
    - [x] Enhance existing terminal-based pseudographics (ASCII/ANSI) to render `WorldSnapshot` as rich as possible in CLI.
    - [x] Build a Web-based (Spring Boot + React) dashboard in the `island-app` module for full graphical visualization.
- [x] **Vector 7: Real-Time Interaction**
    - [x] Allow dynamic pausing, speed adjustments (tick rate scaling), and manual entity spawning via UI (CLI and Web).
- [x] **Vector 8: Serialization & Save States**
    - [x] Implement serialization to save and load `WorldSnapshot` (JSON or binary formats).

## 🚀 Phase 5: Production Readiness & Quality Hardening (Completed)
- [x] **Vector 9: CI/CD & Automation**
    - [x] GitHub Actions CI: Automated pipeline established for all modules.
- [x] **Vector 10: Quality Gate Hardening**
    - [x] JaCoCo coverage threshold increased to 65% project-wide.
- [x] **Vector 11: Modular Isolation**
    - [x] Correctly configured module boundaries and exports/opens for Spring and Jackson (JPMS).
