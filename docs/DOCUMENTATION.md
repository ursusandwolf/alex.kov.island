# Technical Documentation: Island Ecosystem Simulator

## Architecture Overview
The system follows a **Modular Monolith** approach with strict separation of concerns using **JPMS (Java Platform Module System)** and **ECS (Entity-Component-System)** patterns.

### Layers (Hexagonal Influence)
1.  **Core Engine (`island-engine`)**: Independent of domain logic. Manages the lifecycle, scheduling (Phase-based), and SoA-based storage.
2.  **Domain Plugins (`island-nature`, `island-simcity`)**: Implement specific simulation rules via `SimulationPlugin` and `EntitySystem`.
3.  **Application (`island-app`)**: Spring Boot-managed host. Orchestrates plugins, provides REST/WebSocket APIs, and manages persistence.

## Implementation Standards
- **Java 21**: Utilizing Virtual Threads (via `ParallelDispatcher`) and Sealed Classes where applicable.
- **ECS Pattern**: Entities are just IDs; data is stored in SoA (Structure of Arrays) for cache-friendly access.
- **Strategy Pattern**: Used for extensibility (e.g., `SocialEffectProvider` in SimCity).
- **Lombok**: Ubiquitous use for boilerplate reduction.

## Database & Infrastructure
- **Persistence**: JPA with H2 (in-memory/file). Snapshots are stored as JSON CLOBs.
- **Observability**: Spring Boot Actuator + Micrometer + Prometheus.
- **Security**: Basic Auth for all mutation and management endpoints.
- **Containerization**: Multi-stage Docker build producing a lean JRE-based image.

## API Specification (v1)
All endpoints are prefixed with `/api/v1/simulation`.

### REST Endpoints
- `POST /start`: Initialize and start a simulation with custom parameters.
- `POST /stop`: Gracefully stop the current simulation.
- `POST /pause` / `POST /resume`: Control the execution flow.
- `GET /status`: Current engine state and metrics.
- `POST /snapshot`: Persist current state to JPA history.
- `GET /history`: List available historical snapshots.
- `POST /seed`: Reinitialize the world from a historical snapshot.

### WebSocket (STOMP)
- **Topic**: `/topic/simulation`
- **Payload**: `WorldSnapshot` (Polymorphic JSON).
- **Broadcast Interval**: Configurable via `sim.broadcastInterval`.

## Testing Strategy
- **Unit/Integration**: JUnit 5 + Mockito.
- **Property-based**: `jqwik` for complex domain invariant verification.
- **ArchUnit**: Enforcing JPMS and layer boundaries.
- **Compatibility**: `Revapi` for API surface tracking.
