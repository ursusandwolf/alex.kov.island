# Changelog

## [2026-04-23]
### Git Cleanup
- Deleted stale/merged local and remote branches (`feature/feeding-logic`, `feature/fox`, `optimization-to-flyweight-pattern-6fce0`, `feature/flyweight-optimization2`, and various `revert-` and non-English branches).
- Renamed `optimization-to-flyweight-pattern-6fce0` to `feature/flyweight-optimization` for better readability and conformity to standards.
- Synchronized local and remote branch tracking.

### Codebase Refactoring
- Added Project Lombok dependency.
- Simplified codebase: removed excessive Javadoc, applied Lombok (@Getter, etc.) to models (`Organism`, `Animal`, `Plant`, `AnimalType`, `SpeciesConfig`, `Cell`, `Chunk`, `Island`) and animal implementations.
- Modernized and cleaned up `SpeciesConfig` (removed legacy methods).
- Improved code compactness while preserving design patterns and core logic.
