# Island Ecosystem Simulator: Roadmap & TODO

## 🚀 Phase 1: Architectural Foundation (Completed)
- [x] **Vector 1: Event-Driven Architecture**
- [x] **Vector 2: ECS (Entity-Component-System) Evolution**

## 🚀 Sprint 3: Advanced ECS & Performance (Completed)
- [x] **Task 1: System Execution Graph**
- [x] **Task 2: ECS Archetypes**
- [x] **Task 3: Final Architectural Cleanup**
- [x] **Task 4: Performance Benchmarking & GC Optimization**

## 🛠 Maintenance & Quality Hardening (In Progress)
- [ ] Increase test coverage for concurrent scenarios.
- [ ] Implement Revapi for API compatibility checks.
- [ ] Add jqwik property-based tests for core logic.
- [ ] Setup PITest mutation testing in CI pipeline.
- [x] **Technical Debt**: Refactor remaining `@Value` properties into `SimulationProperties`.
- [ ] **Test Coverage**: Fix `@Disabled` test `SnapshotHistoryServiceTest.testLoadSnapshotSuccess` (serialization complexity).
- [x] **Infrastructure**: Implement SpringDoc OpenAPI for API documentation.
- [x] **Infrastructure**: Configure Spring Boot Actuator for metrics and health monitoring.
- [ ] **Operations**: Create Dockerfile and docker-compose.yml for production deployment.
- [ ] **Security**: Implement Basic Authentication with Spring Security.
- [ ] **Persistence**: Migrate snapshot storage from FS to JPA/H2.
- [ ] **Validation**: Fully configure `@ConfigurationPropertiesBinding` (@Validated).

## 🌍 Phase 3 & 4 (Completed)
- [x] Vector 4: Climate & Global Systems
- [x] Vector 5: Headless & API
- [x] Vector 6: Visualization & App Layer
- [x] Vector 7: Real-Time Interaction
- [x] Vector 8: Serialization & Save States
- [x] Vector 9: CI/CD & Automation
- [x] Vector 10: Quality Gate Hardening
- [x] Vector 11: Modular Isolation
