# Project Context: Island Ecosystem Simulator

## Status: Production-Ready (v1.56.0)
The project has successfully transitioned from a library-first prototype to a full-stack containerized application with robust security, persistence, and observability.

## Project Goal
To provide a high-performance, extensible engine for simulating complex ecosystems and urban environments, leveraging modern Java features and ECS architecture.

## System State (Summary)
- **Engine**: Stable, optimized (Zero-GC hot path), and modular.
- **Domains**: Nature (predatory/metabolic) and SimCity (urban/social) are fully integrated.
- **Backend**: Spring Boot 3.2.5 with Basic Auth and JPA/H2 storage.
- **Observability**: Prometheus metrics enabled; multi-stage Docker setup ready.

## Technical Entry Point
For detailed architectural patterns, API specs, and implementation standards, refer to:
👉 **[DOCUMENTATION.md](DOCUMENTATION.md)**

## Roadmap & Pending Items
1.  **Observability Phase 2**:
    *   Implement pre-configured Grafana dashboards for domain-specific metrics.
    *   Add ELK/Loki for structured logging analysis.
2.  **Domain Expansion**:
    *   Develop "Deep Sea" plugin with fluid dynamics and light-based metabolic cycles.
    *   Implement "Space" plugin for orbital mechanics and resource management.
3.  **Quality Hardening**:
    *   Expand `jqwik` property-based tests to cover entity movement and reproduction race conditions.
    *   Implement Mutation Testing (PITest) to verify test suite effectiveness.
