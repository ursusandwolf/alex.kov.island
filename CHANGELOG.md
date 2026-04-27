# Changelog

## [2026-04-27]
### Stabilization & UI Refinement
- **Code Duplication & Architectural Cleanup**:
    - **Unified Service Hierarchy**: Integrated `FeedingService` into `AbstractService`, eliminating redundant parallel processing logic.
    - **Polymorphic Lifecycle**: Introduced `Biomass.tick(Cell)` to unify plant growth and insect metamorphosis cycles, removing `instanceof` checks from `LifecycleService`.
    - **Offspring Logic Refactoring**: Replaced manual type checking in `ReproductionService` with a polymorphic `getOffspringBonus()` method in the `Animal` class.
    - **Dead Code Elimination**: Removed unused methods (`canOnlyEat`, `checkState`, `ageOneTick`) from the core `Organism` model to improve maintainability.
- **Console UI & Rendering Optimization**:
    - **Flicker-Free Dashboard**: Optimized `ConsoleView` to use terminal cursor-positioning and line-clearing escape codes, providing 100% smooth animation.
    - **Zero-Allocation Rendering**: Replaced heavy object allocation and Stream API usage in `renderCell` with a pre-allocated primitive array for O(1) performance.
    - **Precision Borders**: Added Unicode borders with corrected 3-character alignment to ensure the map remains perfectly rectangular.
    - **Enhanced Sparklines**: Improved `ViewUtils.getSparkline` visibility by using `.` for base levels and removing debug characters.

### Biomass Unification & Swarm Dynamics
- **Unified Biomass Abstraction**: Replaced the specific `Plant` class with a more versatile `Biomass` base class. 
    - **Shared Logic**: All mass-based organisms (Grass, Cabbage, Caterpillar, Butterfly) now share a unified growth, consumption, and storage model.
    - **O(1) Species Access**: Refactored `Cell` to store biomass in an `EnumMap`, providing constant-time access for feeding and lifecycle services.
- **Swarm Movement Implementation**: Introduced mobile biomass dynamics.
    - **`moveBiomass` Logic**: `Island` now supports atomic transfer of entire species "swarms" between cells, enabling movement for butterflies.
    - **Enhanced `MovementService`**: Butterflies now utilize their speed attribute (defined in `species.properties`) to traverse the island via a 25% mass-flow "diffusion" model.
- **Closed Biological Loop**: Butterflies now reproduce by converting a portion of their biomass back into caterpillars, ensuring life cycle continuity.
- **Architectural Cleanup**:
    - **Simplified Energy Costs**: Refactored animal movement costs to a unified formula `(1 + speed) * coef`, eliminating redundant constants.
    - **Metabolism Refactoring**: Unified `getDynamicMetabolismRate` in `Organism` using a template method pattern for specialized modifiers (e.g., Herbivore bonus), eliminating `instanceof` checks in hot loops.
    - **Registry-Driven Mobility**: Added `plantSpeed` to `SpeciesRegistry` and `SpeciesLoader`, allowing biomass mobility to be configured externally.
    - **Test Synchronization**: Updated `FeedingServiceTest` and `BiologicalPendulumTest` to reflect the new biomass container architecture, ensuring full suite stability.

### Biological Diversity & Lifecycle 
- **Hamster Species**: Added `Hamster` (🐹) with optimized small-mammal metrics (0.1kg weight, high density, mouse-like diet).
- **Complex Metamorphosis**: Implemented a staged life cycle for `Caterpillar` (🐛) and `Butterfly` (🦋). 
    - **Staged Biomass**: Refactored `Caterpillar` to use an internal state-bucket system (40 ticks active, 20 ticks sleep).
    - **Pupation**: 50% of maturing caterpillars enter a 20-tick hibernation phase before emerging as butterflies.
    - **Unique Predation**: Butterflies are now part of the ecosystem biomass, specifically targeted only by Ducks (🦆).
- **Population Pyramid Initialization**: Rebalanced `WorldInitializer` to implement a density-based "pyramid" (TINY: 60-95%, SMALL: 40-70%, NORMAL: 20-45%), ensuring a sustainable food base for predators from tick 1.

### Architectural Stabilization & Consistency
- **Unified Energy Policy**: Introduced `EnergyPolicy` Enum to replace loose constants. Centralized thresholds for action (15%), reproduction (70%), and escape energy loss (5%).
- **Finalized Simulation Constants**: Converted all `SimulationConstants` to `static final` to prevent runtime mutation and ensure thread-safety.
- **Improved Size Classification**: Refined `SizeClass` thresholds to align Duck (1.0kg) with the base `NORMAL` metabolism (1.00x), and categorized Hamsters/Mice as `TINY` for high reproduction.
- **Biomass Statistics Engine**: Rewrote `Island.getSpeciesCounts()` and `getTotalOrganismCount()` to accurately sum total mass (kg) for biomass species, ensuring dashboard numbers exactly match the population list.
- **Safe Extinction Monitoring**: Updated `SimulatorMain` to exclude biomass species from the global extinction check, preventing premature simulation termination when caterpillars pupate or plants are grazed.

### Architectural Refinement & Optimization
- **Decoupled Species Management**: Fully eliminated the `SpeciesConfig` Singleton. Replaced it with an immutable `SpeciesRegistry` and a dedicated `SpeciesLoader`. All dependencies are now handled via constructor injection, significantly improving testability and adhering to SRP.
- **ROI-Driven Hunting Optimization**: Refactored `PreyProvider` to utilize the new `SizeClass` indexing in `Cell`. Predators now scan potential prey starting from the largest size (HUGE -> SMALL), drastically reducing loop iterations in `FeedingService` by reaching satiety sooner.
- **Mass-Injected Plant Models**: Plants (`Grass`, `Cabbage`) and `Caterpillar` biomass containers now receive their configuration parameters via constructor injection, removing hidden static dependencies.
- **Red Book Stabilization**: Added a toggle for "Red Book" protection to ensure deterministic test results while maintaining the ecosystem protection logic in production.
- **Complete Test Overhaul**: Synchronized 20 core unit and integration tests with the new registry-based architecture, ensuring 100% pass rate.

### Stabilization & Critical Fixes
- **Race Condition Resolution**: Fixed a critical bug in `FeedingService` where multiple predators could consume the same prey. Implemented atomic check-and-consume logic using synchronized cell locking.
- **Ecosystem Re-balancing**: Restored all species parameters to `FULL_TASK.md` baseline. Corrected caterpillar saturation limits and adjusted plant growth rates to 10% for long-term stability.
- **Config Validation**: Added strict validation for all configuration parameters, preventing simulation crashes due to non-positive dimensions or invalid species metrics.

### Architectural Refinement (SOLID & Patterns)
- **Strategy Pattern for Hunting**: Extracted hunting logic into `HuntingStrategy` and `DefaultHuntingStrategy` (OCP). Predators now use an extensible ROI-based evaluation for prey selection.
- **Enhanced Cell Model**: Replaced linear list scanning in `Cell` with indexed storage using `EnumMap<SpeciesKey, List<Animal>>` and role-based buckets. Optimized access time to O(1) for frequent operations.
- **Unified Death Lifecycle**: Centralized mortality logic in `Organism.tryConsumeEnergy()`. Introduced `DeathCause` enum and detailed per-species mortality tracking (Hunger, Age, Eaten, Exhaustion).
- **Decoupled Config Management**: Split `SpeciesConfig` into `SpeciesLoader` (loading) and `SpeciesRegistry` (immutable storage), strictly following SRP.
- **Typed Domain Model**: Replaced magic strings with `SpeciesKey` enum across the entire codebase. Replaced raw types in collections with parameterized generics for better type safety.
- **Size Classification**: Introduced `SizeClass` (Small to Huge) to unify weight thresholds for metabolism and movement costs.

### Code Quality & Standards
- **Checkstyle Integration**: Integrated `maven-checkstyle-plugin` with a customized Google Style guide. Brought the entire codebase to 0 style violations.
- **Optimized Hot Loops**: Removed expensive `Collections.shuffle()` and redundant sorting from simulation cycles to reduce CPU overhead and improve tick performance.
- **Extended Test Coverage**: Added `EcosystemStabilityTest` (long-run verification) and `BoundaryConditionsTest`. Updated and stabilized all existing unit tests to pass 100% green.
- **Enhanced Statistics**: Updated `ConsoleView` to display cumulative mortality metrics and animal-only death counters (excluding plants/biomass from death totals).

## [2026-04-26]
### Modernization & Scalability
- **Java 21 Migration**: Upgraded project from Java 17 to Java 21 LTS to leverage the latest JVM features and performance improvements.
- **Virtual Threads (Project Loom)**: Replaced traditional `FixedThreadPool` with `VirtualThreadPerTaskExecutor` in `GameLoop`. This allows for near-infinite scalability of parallel simulation tasks (chunks) without the overhead of platform threads.
- **Extreme Performance Optimization**:
    - **Smart Biomass**: Refactored `Caterpillar` and `Plant` into mass-based containers, removing up to 2 million individual Java objects from simulation cycles.
    - **Hot Loop Refactoring**: Replaced all Stream API calls and expensive `CopyOnWriteArrayList` instances with optimized `ArrayList` and basic `for-i` loops in critical services (`Feeding`, `Movement`, `Lifecycle`).
    - **Lombok-Free Core**: Replaced annotations with manual getters in model classes to ensure 100% reliable behavior under high concurrency and Java 21.

### Advanced Biological Model
- **Biological Pendulum**: Implemented a realistic biomass cycle where Caterpillars consume Plants to grow, and return mass as "fertilizer" to the Grass layer upon decay.
- **Kleiber’s Law (Stepped)**: Introduced discrete metabolic modifiers based on weight categories (Tiny: 1.2x, Medium: 1.0x, Large: 0.8x) and a survival bonus for Herbivores (0.8x).
- **Rational Hunting (ROI)**: Predators now act as efficient hunters, skipping targets if the expected energy gain (gain * probability) doesn't cover the effort (strike + chase + fatigue).
- **Hunt Fatigue**: Implemented progressive exhaustion where energy costs increase by 30% for every 5 hunt attempts in a single tick.
- **Bear Hibernation**: Added a unique life cycle for Bears: starting with 50 ticks of energy-free sleep followed by 100 ticks of activity, providing "windows of recovery" for prey.
- **Red Book Protection**: Automatic stealth mechanism and reproduction/mobility bonuses for species whose global population falls below 5% of island capacity.
- **Energy Redistribution**: Parents and offspring now share total available energy equally during reproduction, ensuring a survival floor of 40% for the whole family.

### Visualization & Analytics
- **Enhanced Dashboard**: Integrated a global population sparkline, a color-coded Satiety progress bar, and real-time death statistics (Hunger vs. Age).
- **Full Species Visibility**: The UI now tracks all 17 species even at 0 population, with automatic letter fallbacks (e.g., 'D' for Deer) if icons are missing.
- **Corrected Stats**: Updated island counters to properly sum both individual animal counts and total plant/insect biomass units.

### Stability & Quality
- **Shutdown Safety**: Eliminated `RejectedExecutionException` by implementing safe-stop checks in `GameLoop` and all parallel services.
- **Targeted Re-balancing**: Normalized initial population densities (10-35% for prey, 2-5% for apex predators) and applied specific reproduction nerfs to Buffalo to prevent over-grazing.
- **Unit Test Suite**: Expanded and stabilized the test suite (19 tests) to cover the new energy redistribution and 'Smart Biomass' logic.
- **Documentation**: Added `UML.md` with pseudographic architecture diagrams and design pattern descriptions.

## [2026-04-25]
### Architecture & SOLID Refactoring
- **Interface Segregation (ISP)**: Deployed `Mobile`, `Consumer`, and `Reproducible` interfaces. Removed the bloated `OrganismBehavior` interface.
- **Dependency Inversion (DIP)**: Refactored `AnimalFactory` and all 15 animal species to use constructor injection for `AnimalType`. Removed hard dependencies on `SpeciesConfig` Singleton inside animal classes.
- **Bootstrap Pattern**: Introduced `SimulationBootstrap` and `SimulationContext` to decouple initialization logic from `SimulatorMain` (SRP).
- **Service Abstraction**: Created `AbstractService` to centralize parallel chunk processing logic, reducing code duplication across all simulation phases.
...
