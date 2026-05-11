# Architecture: Island Ecosystem Simulator

## Core Engine Overview

The Island Ecosystem Simulator is built on a high-throughput, plugin-oriented core engine. It utilizes a phase-based scheduling mechanism to handle concurrent simulation steps while maintaining determinism.

```text
+-----------------------+           +--------------------------+
|   Simulation Engine   |<----------|     Plugin Interface     |
| (Lifecycle Management)|           | (Nature, SimCity, etc.)  |
+-----------+-----------+           +------------+-------------+
            |                                    ^
            v                                    |
+-----------------------+           +------------+-------------+
|      Game Loop        |---------->|    Parallel Dispatcher   |
| (Phase Management)    |           |   (Virtual Threads)      |
+-----------------------+           +------------+-------------+
            |
            v
+-----------------------+
|   Simulation World    |
| (Grid/Cell Structure) |
+-----------------------+
```

### Core Components
- **GameLoop**: Coordinates simulation phases (Prepare, Simulation, Post-Process) using `PhaseScheduler`.
- **ParallelDispatcher**: Manages execution of tasks. Uses Virtual Threads for high-concurrency throughput and load-balancing.
- **EventBus**: Decoupled messaging system. Facilitates communication between domain services and the core simulation heartbeat.

---

## Plugin Architecture

Plugins encapsulate domain-specific logic. Each plugin follows a strict separation of concerns, ensuring the engine remains agnostic of the actual simulation rules.

```text
+----------------------+           +----------------------+
|     Plugin Entry     |---------->|      Domain Logic    |
| (Registration/Init)  |           | (Services, Entities) |
+----------------------+           +-----------+----------+
                                               |
                                               v
+----------------------+           +----------------------+
|    Domain View       |           |   Domain Model (ECS) |
| (Console/Rendering)  |<----------| (Cells, Components)  |
+----------------------+           +----------------------+
```

### Key Principles
1. **DIP (Dependency Inversion Principle)**: Domain services (`FeedingService`, `ReproductionService`) depend on interfaces provided by the `SimulationEngine` (e.g., `SimulationWorld`, `EventBus`).
2. **Phase-based Execution**: Plugins register `ScheduledTask`s that the `GameLoop` executes during the appropriate phase, preventing race conditions.
3. **ECS Pattern**: Entities hold state via `Component`s, allowing for lightweight, modular behavior extensions without complex inheritance trees.
