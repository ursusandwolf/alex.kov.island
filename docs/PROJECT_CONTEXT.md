# Project Context

## Current State
- **Major Refactoring (May 2026)**: 
    - Reorganized `com.island.nature.entities`, `com.island.engine`, and `com.island.util` into specialized sub-packages (core, registry, strategy, scheduling, etc.) to maintain a clean directory structure (< 10 files per folder).
    - Refactored simulation services in `com.island.nature.service` to use a Template Method pattern in `AbstractService`, significantly reducing duplication and improving type safety.
    - Standardized service constructors to use `ExecutorService` and `RandomProvider` while removing the unused `EventBus`.
    - Cleaned up imports and removed FQNs across the entire source tree.

## Technical Debt / Known Issues
- **Test Suite**: The tests have been structurally updated to match new packages, but the test-classes currently face compilation errors due to complex mock setups and service instantiation mismatches in edge-case tests.
- **Lombok Usage**: While improved, some classes still have redundant boilerplate that could be further reduced with `@RequiredArgsConstructor` or `@Slf4j`.

## Pending Items
- Resolve remaining test compilation errors in `src/test/java`.
- Finalize the migration of `MovementService` to a fully component-based ECS approach as planned.
- Update UML diagrams to reflect the new package structure.
