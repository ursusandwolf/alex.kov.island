# Project Context

## Current State
- The project is an Island Ecosystem Simulator.
- Code Review v4 issues have been addressed (double reporting, thread-safety, EventBus robustness).
- Global and project-specific `GEMINI.md` files are configured.

## Recent Changes
- Fixed double death reporting by centralizing `EntityDiedEvent` publication in `Island.onEntityRemoved`.
- Replaced `HashMap` with `ConcurrentHashMap` in `Organism` for thread-safe component storage.
- Added error isolation to `DefaultEventBus`.
- Refactored `EventBus` injection to use constructor-based DI and enforce immutability in `SimulationWorld`.
- Updated all test suites to accommodate constructor changes.
- Updated documentation (CHANGELOG, DOCUMENTATION, UML).

## Pending Items
- Continue simulation development or refactoring as requested.
