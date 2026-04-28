# Island Ecosystem Simulator Architecture (UML)

## Class Diagram Overview

```mermaid
classDiagram
    %% Core Abstractions
    class Organism {
        <<Abstract>>
        -id: String
        -currentEnergy: double
        -maxEnergy: double
        -age: int
        -isAlive: boolean
        +checkAgeDeath() boolean
        +consumeEnergy(amount)
        +getSpeciesKey() SpeciesKey
        +getDynamicMetabolismRate() double
    }

    class Animal {
        <<Abstract>>
        -animalType: AnimalType
        +move() boolean
        +reproduce() Animal
        +getWeight() double
    }

    class Biomass {
        <<Abstract>>
        -biomass: double
        -maxBiomass: double
        -typeName: String
        +grow()
        +consumeBiomass(amount) double
    }

    %% Hierarchy and Interfaces
    class Predator {
        <<Interface>>
        +isAnimalPredator() boolean
    }
    class Herbivore {
        <<Interface>>
        +isAnimalHerbivore() boolean
    }

    Organism <|-- Animal
    Organism <|-- Biomass
    
    Animal <|-- AbstractPredator
    Animal <|-- AbstractHerbivore
    
    AbstractPredator ..|> Predator
    AbstractHerbivore ..|> Herbivore
    
    AbstractPredator <|-- Wolf
    AbstractPredator <|-- Bear
    Bear ..|> Herbivore : "Omnivore"
    
    AbstractHerbivore <|-- Rabbit

    Biomass <|-- Grass
    Biomass <|-- Cabbage
    Biomass <|-- Caterpillar
    Biomass <|-- Butterfly

    %% Model and Engine
    class Island {
        -grid: Cell[][]
        -chunks: List~Chunk~
        -speciesCounts: Map~SpeciesKey, AtomicInteger~
        +getProtectionMap() Map
    }

    class Cell {
        -x, y: int
        -predators: List~Animal~
        -herbivores: List~Animal~
        -biomassBySpecies: Map~SpeciesKey, Biomass~
        -lock: ReentrantLock
        +addAnimal(animal)
        +addBiomass(biomass)
    }

    class GameLoop {
        -taskExecutor: ExecutorService (Virtual Threads)
        -timer: ScheduledExecutorService
        +start()
        +stop()
    }

    %% Design Patterns
    note for Organism "Base Entity with Energy State"
    note for AnimalType "Flyweight: Common species data"
    note for PreyProvider "Mediator: Orchestrates hunting buffet"
    note for HuntingStrategy "Strategy: Encapsulates hunt decision logic (ROI-based)"
    note for InteractionMatrix "High-performance Interaction Lookup (O(1))"
```

## System Patterns Applied

1.  **Flyweight (`AnimalType`)**: 
    - Static data (weight, speed, max count) for each species is stored once per type.
    - Saves massive memory with millions of organisms.

2.  **Strategy (`HuntingStrategy`)**:
    - Decouples prey selection logic from `FeedingService`.
    - Predators use ROI (Return on Investment) calculations to prioritize prey.

3.  **Mediator (`PreyProvider`)**:
    - Centralizes predator-prey interaction logic within a cell.
    - Manages "hiding" state and dynamic "buffet" generation.

4.  **Composite (`Island` -> `Chunk` -> `Cell`)**:
    - Hierarchy that allows processing by chunks in parallel.

5.  **Virtual Threads (Project Loom)**:
    - High-performance task execution in `GameLoop`.
    - Near-infinite scalability for parallel cell processing.

6.  **Hybrid Hierarchy**:
    - Combination of Abstract Classes (performance) and Interfaces (flexibility).
    - Supports Omnivores (like Bear) through multiple interface implementation.

## Key Mechanisms

- **Biological Pendulum**: Cyclic biomass flow between Plants and Caterpillars (Feeding/Fertilizing).
- **Red Book Protection**: Automatic stealth mechanism for endangered species (pop < 5% of island capacity).
- **Energy Redistribution**: Parents and offspring share total energy during reproduction.
- **Ordered Locking**: Prevents deadlocks during animal movement between cells.
