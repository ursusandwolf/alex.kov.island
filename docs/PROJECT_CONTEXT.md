# Project Context

## Current State
- **Phase 5: Production Readiness & Quality Hardening (May 2026)**:
    - **SoA Correctness**: Replaced `volatile` arrays with `AtomicLongArray` and `AtomicIntegerArray` in `HealthSoAStore` and `AgeSoAStore`.
    - **Movement SoA**: `MovementSoAStore` now uses `StampedLock` for thread-safe resizing and element access.
    - **CityTile Hardening**: All entity management methods in `CityTile` are now protected by a `ReentrantLock`.
    - **SimCity Environmental Systems**:
        - **Desirability Map**: Implemented `DesirabilityService` to calculate tile appeal based on infrastructure and pollution.
        - **Zoning Densities**: `BuildingComponent` now supports LOW, MEDIUM, and HIGH density.
        - **Wealth Tiers**: `PopulationComponent` supports POOR, MIDDLE, and WEALTHY levels.
        - **Progression Logic**: `ZoningService` handles building upgrades and wealth progression based on environmental factors.
    - **Scheduling Improvements**: `ConnectivityService`, `PollutionService`, and `DesirabilityService` moved to `Phase.PREPARE` for better data consistency.
    - **Performance**: Optimized `ConnectivityService` to use `BitSet` for pathfinding and reduced GC pressure in several services.

## Architecture
- **Engine**: Decoupled core with SoA-based storage and phase-based scheduling.
- **Nature**: High-performance ecosystem with predatory and metabolic logic.
- **SimCity**: Grid-based urban simulation with RCI zones, infrastructure, and environmental mechanics.

## Next Steps
- Further optimize `SystemExecutionGraph` for multi-core scaling.
- Investigate native memory usage (off-heap) for massive SoA stores.
- Expand `EcosystemBalanceTest` with edge-case climate scenarios (volcanic winter, heat waves).
- Improve JaCoCo coverage to reach 75% threshold (currently 70%).
- Implement JMH benchmarks for EventBus and ECS query performance.
