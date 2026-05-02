# TODO: Architecture & Code Quality Improvements

## High Priority: Decoupling & Dependency Injection
- [x] **Eliminate Static Constants Dependency**: Refactor `Organism`, `AnimalType`, `Cell`, and services to accept a `Configuration` object instead of using static imports from `SimulationConstants`.
- [x] **Introduce Context Injection**: Ensure that all domain-specific components (registries, factories) are provided via constructor injection, enabling multi-instance simulations.

## Medium Priority: Service Refinement
- [x] **Refactor StatisticsService**: Split large methods (Extract Method) and improve data aggregation logic.
- [x] **Enhanced Monitoring**: Replace `Thread.sleep` in `NatureLauncher` with a `ScheduledExecutorService` for more precise and non-blocking monitoring.
- [x] **Biomass Logic Optimization**: Review and simplify the `BiomassManager` implementation in `Island.java`.

## Low Priority: Cleanup & Consistency
- [x] **Javadoc Audit**: Add missing Javadocs to the new specialized interfaces (`NatureRegistry`, `NatureStatistics`, etc.).
- [ ] **SimCity Consistency**: Ensure that any architectural wins from the Nature refactoring (like narrow interfaces) are applied to SimCity where applicable.
- [x] **Logging Modernization**: Move from `System.out.println` to a structured logging framework (e.g., SLF4J/Logback).
