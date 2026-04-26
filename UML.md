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
        #isHiding: boolean
        +checkAgeDeath()* boolean
        +consumeEnergy(amount)*
        +getSpeciesKey()* String
    }

    class Animal {
        <<Abstract>>
        -animalType: AnimalType
        +move() boolean
        +reproduce() Animal
        +getWeight() double
    }

    class Plant {
        <<Abstract>>
        -biomass: double
        -maxBiomass: double
        +grow()
        +consumeBiomass(amount) double
    }

    %% Relationships
    Organism <|-- Animal
    Organism <|-- Plant
    Animal <|-- Wolf
    Animal <|-- Rabbit
    Plant <|-- Grass
    Plant <|-- Caterpillar : "Optimized Biomass"

    %% Model and Engine
    class Island {
        -grid: Cell[][]
        -chunks: List~Chunk~
        -speciesCounts: Map
        +getProtectionMap() Map
        +nextTick()
    }

    class Cell {
        -x, y: int
        -animals: List~Animal~
        -plants: List~Plant~
        -lock: ReentrantLock
        +addAnimal(animal)
        +getPlantCount() int
    }

    class GameLoop {
        -taskExecutor: ExecutorService (Virtual Threads)
        -timer: ScheduledExecutorService
        +start()
        +stop()
    }

    %% Design Patterns
    note for Organism "Base Entity"
    note for AnimalType "Flyweight Pattern: Common species data"
    note for PreyProvider "Mediator Pattern: Orchestrates hunting buffet"
    note for WorldInitializer "Builder/Factory: Sets up initial state"
```

## System Patterns Applied

1.  **Flyweight (`AnimalType`)**: 
    - Static data (weight, speed, max count) for each species is stored once per type.
    - Saves massive memory with millions of organisms.

2.  **Mediator (`PreyProvider`)**:
    - Centralizes predator-prey interaction logic within a cell.
    - Manages "hiding" state and dynamic "buffet" generation.

3.  **Composite (`Island` -> `Chunk` -> `Cell`)**:
    - Hierarchy that allows processing by chunks in parallel.

4.  **Virtual Threads (Project Loom)**:
    - High-performance task execution in `GameLoop`.
    - Near-infinite scalability for parallel cell processing.

5.  **Smart Biomass (Optimization)**:
    - `Caterpillar` and `Plant` act as containers, reducing object count from millions to thousands.

## Key Mechanisms

- **Biological Pendulum**: Cyclic biomass flow between Plants and Caterpillars (Feeding/Fertilizing).
- **Red Book Protection**: Automatic stealth mechanism for endangered species (pop < 5%).
- **Energy Redistribution**: Parents and offspring share total energy during reproduction.
