# TODO: Architecture & Code Quality Improvements

## High Priority: Decoupling & Dependency Injection
- [x] **Eliminate Static Constants Dependency**: Refactor `Organism`, `AnimalType`, `Cell`, and services to accept a `Configuration` object instead of using static imports from `SimulationConstants`.
- [ ] **Introduce Context Injection**: Ensure that all domain-specific components (registries, factories) are provided via constructor injection, enabling multi-instance simulations.

## Medium Priority: Service Refinement
- [ ] **Refactor StatisticsService**: Split large methods (Extract Method) and improve data aggregation logic.
- [ ] **Enhanced Monitoring**: Replace `Thread.sleep` in `NatureLauncher` with a `ScheduledExecutorService` for more precise and non-blocking monitoring.
- [ ] **Biomass Logic Optimization**: Review and simplify the `BiomassManager` implementation in `Island.java`.

## Low Priority: Cleanup & Consistency
- [ ] **Javadoc Audit**: Add missing Javadocs to the new specialized interfaces (`NatureRegistry`, `NatureStatistics`, etc.).
- [ ] **SimCity Consistency**: Ensure that any architectural wins from the Nature refactoring (like narrow interfaces) are applied to SimCity where applicable.
- [ ] **Logging Modernization**: Move from `System.out.println` to a structured logging framework (e.g., SLF4J/Logback).
