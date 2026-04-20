# Island Ecosystem Simulator - Development Progress

## Overview
Interactive educational project for building a multithreaded ecosystem simulator using Java, OOP, and design patterns (GOF/GRASP).

**Current Status**: Phase 1 Complete - Core classes created with TODO stubs for student implementation.

---

## ✅ Created Files (15 Java Classes)

### Content Layer (`com.island.content`)

| File | Description | Status |
|------|-------------|--------|
| `OrganismBehavior.java` | Interface with default methods (`canPerformAction`, `canOnlyEat`) | ✅ Complete |
| `Organism.java` | Abstract base class (energy, age, life status, ID) | ✅ Complete |
| `Animal.java` | Abstract animal class (weight, speed, diet, movement) | ✅ Complete |
| `Plant.java` | Abstract plant class (biomass, growth) | ✅ Complete |
| `Predator.java` | **Marker interface** - predators move first in tick | ✅ NEW |
| `Herbivore.java` | **Marker interface** - herbivores move second | ✅ NEW |
| `SpeciesConfig.java` | Singleton configuration + probability matrix | ⏳ Needs matrix data |
| `AnimalFactory.java` | Factory Method for creating animals by species key | ✅ Complete |

### Animal Implementations (`com.island.content.animals`)

| File | Type | Interface | Status |
|------|------|-----------|--------|
| `Wolf.java` | Predator | `Predator` | ✅ Example with TODOs |
| `Rabbit.java` | Herbivore | `Herbivore` | ✅ Example with TODOs |
| `Duck.java` | Herbivore | `Herbivore` | ✅ NEW - Dual diet (plants + caterpillars) |
| `Caterpillar.java` | Herbivore | `Herbivore` | ✅ NEW - Stationary (speed=0) |

**Still need to create:**
- Predators: Snake (Удав), Fox (Лисиця), Bear (Ведмідь), Eagle (Орел)
- Herbivores: Horse, Deer, Mouse, Goat, Sheep, Boar, Buffalo

### Model Layer (`com.island.model`)

| File | Description | Status |
|------|-------------|--------|
| `Cell.java` | Thread-safe cell with `ReentrantLock`, separate animal/plant lists | ✅ Complete + `consumePlants()` stub |
| `Chunk.java` | Chunk for parallel processing (4-phase tick logic) | ✅ Structure ready, needs implementation |
| `Island.java` | 2D grid (100×20), toroidal wrap-around, chunk management | ✅ Structure ready, movement has stub |

---

## 🎯 Key Design Decisions

### 1. Execution Order (Priority System)

```
Phase 1: EAT - Sorted by type then speed
├── Predators (implements Predator interface)
│   ├── Wolf (speed 3)
│   ├── Eagle (speed 3)
│   ├── Fox (speed 2)
│   ├── Bear (speed 2)
│   └── Snake (speed 1)
└── Herbivores (implements Herbivore interface)
    ├── Duck (speed 4) ← Moves FIRST among herbivores!
    ├── Horse (speed 4)
    ├── Deer (speed 4)
    ├── ...
    └── Caterpillar (speed 0) ← Always last, stationary
```

**Why this matters:** Duck (speed 4) eats caterpillars BEFORE slower predators can compete for them. This matches the specification requirement.

### 2. Dual-Diet Logic (Duck Example)

```java
// Duck.eat() implements two-phase eating:
// Phase 1: Hunt caterpillars (90% success rate)
for (Animal prey : cell.getAnimals()) {
    if (prey instanceof Caterpillar) {
        if (rollSuccess(90)) {
            eat(prey);
            if (isSatisfied()) return;
        }
    }
}
// Phase 2: Eat plants if still hungry
cell.consumePlants(neededAmount, this);
```

### 3. Thread Safety Strategy

- **Cell-level locking**: Each `Cell` has its own `ReentrantLock`
- **Lock ordering**: Lock cells by coordinates (smaller X,Y first) to prevent deadlocks during movement
- **CopyOnWriteArrayList**: Safe iteration over organisms during concurrent modifications
- **Chunk isolation**: Each thread processes one chunk independently

### 4. Energy System

```
Energy Levels:
├── 100% = Full energy (after eating to saturation)
├── ≥30% = Can perform ALL actions (eat, move, reproduce)
├── 0-30% = Can ONLY eat (survival mode)
└── 0% = Death

Action Costs (per action):
├── Eat: 5% energy
├── Move: 5% energy
└── Reproduce: 5% energy + 50% energy split with offspring
```

---

## 🔧 TODO Items for Student

### High Priority (Core Mechanics)

1. **[ ] Fill Probability Matrix** in `SpeciesConfig.initializeProbabilityMatrix()`
   - Need all 16×16 values from specification table
   - Example: `setProbability("wolf", "rabbit", 60)`
   - This is tedious but foundational - do it first!

2. **[ ] Implement `Cell.consumePlants(double amount, Object consumer)`**
   - Loop through plants list
   - Reduce each plant's biomass proportionally
   - Update `totalPlantBiomass` cache
   - Return actual amount consumed

3. **[ ] Implement `Island.moveOrganism()`**
   - Uncomment the lock-ordering code
   - Test with simple cases first
   - Handle edge case: destination cell full

4. **[ ] Complete `Chunk.processTick()`**
   - Phase 1: Sort animals (predators first, then by speed)
   - Phase 2: Call `animal.eat()` for each
   - Phase 3: Call `animal.move()` for each
   - Phase 4: Call `animal.reproduce()` for each
   - Phase 5: Cleanup dead organisms

5. **[ ] Create Remaining 11 Animal Species**
   - Use Wolf/Rabbit as templates
   - Copy exact stats from specification table
   - Implement appropriate interface (`Predator` or `Herbivore`)

### Medium Priority (Engine)

6. **[ ] Create `SimulationEngine.java`**
   - `ScheduledExecutorService` for main loop
   - `ExecutorService` (thread pool) for chunk processing
   - Coordinate 4 phases across all chunks

7. **[ ] Create `Statistics.java`**
   - Track population per species
   - Track births/deaths per tick
   - Output to console every 5 ticks

8. **[ ] Implement Priority Sorting**
   - In `Chunk.processTick()`, sort animals before processing
   - Use `instanceof Predator` for type check
   - Then sort by `getSpeed()` descending

### Low Priority (Enhancements)

9. **[ ] Unique ID Generation**
   - Replace `System.nanoTime()` with `AtomicLong` counter
   - Format: sequential numbers or UUID

10. **[ ] Console Visualization**
    - Simple ASCII art showing cell contents
    - Or graph per-species populations over time

---

## 📊 Implementation Checklist

### Phase 1: Content (Current Progress: 4/15 animals)
- [x] Base classes (Organism, Animal, Plant)
- [x] Marker interfaces (Predator, Herbivore)
- [x] Example animals (Wolf, Rabbit)
- [x] Special cases (Duck, Caterpillar)
- [ ] Snake, Fox, Bear, Eagle
- [ ] Horse, Deer, Mouse, Goat, Sheep, Boar, Buffalo
- [ ] Fill probability matrix

### Phase 2: World Model (Current Progress: Structure only)
- [x] Cell with thread safety
- [x] Island grid with wrap-around
- [x] Chunk structure
- [ ] Implement consumePlants()
- [ ] Implement moveOrganism()
- [ ] Initialize world (15-45% fill rate)

### Phase 3: Engine (Current Progress: Not started)
- [ ] SimulationEngine with thread pools
- [ ] 4-phase tick coordination
- [ ] Statistics collection
- [ ] Main entry point

### Phase 4: Testing & Polish
- [ ] Test small world (10×5)
- [ ] Verify probability mechanics
- [ ] Balance tuning
- [ ] Documentation

---

## 🚀 Recommended Next Steps

**For your next coding session, follow this order:**

1. **Start with SpeciesConfig** (30 min)
   - Fill the probability matrix completely
   - This unblocks all eating logic

2. **Implement Cell.consumePlants()** (20 min)
   - Simple loop, easy to test
   - Lets you verify plant-eating works

3. **Add 2 More Animals** (40 min)
   - Try Fox (predator) and Deer (herbivore)
   - Practice with inheritance pattern

4. **Test Movement** (30 min)
   - Uncomment Island.moveOrganism()
   - Create tiny test: 2 cells, 1 animal moving

5. **Build Minimal Engine** (60 min)
   - Single-threaded first
   - One tick cycle
   - Print results

Total: ~3 hours to get basic simulation running!

---

## 📝 Code Review Notes

When submitting for code review, highlight:

1. **OOP Principles**: Show how Wolf/Rabbit/Duck inherit from Animal
2. **Design Patterns**: Point out Factory, Template Method, Strategy usage
3. **Thread Safety**: Explain ReentrantLock and lock ordering
4. **Extensibility**: Demonstrate adding new species is easy

Common questions to prepare for:
- "Why use marker interfaces instead of enum?" → Runtime type checking for sorting
- "How do you prevent deadlocks?" → Consistent lock ordering by coordinates
- "What if two threads modify same cell?" → ReentrantLock ensures mutual exclusion

---

*Last updated: Interactive development session in progress*
