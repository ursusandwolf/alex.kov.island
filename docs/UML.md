# Архитектура проекта (UML)

## Общая схема модулей и зависимостей

```mermaid
graph TD
    subgraph App
        AL[NatureLauncher]
        SL[SimCityLauncher]
    end

    subgraph Plugins
        NP[island-nature]
        SP[island-simcity]
    end

    subgraph Core
        IE[island-engine]
        IU[island-util]
    end

    AL --> NP
    SL --> SP
    NP --> IE
    SP --> IE
    IE --> IU
    NP --> IU
    SP --> IU
```

## ECS & SoA Architecture (Engine)

```mermaid
classDiagram
    class Entity {
        +long entityId
        +addComponent(Component)
        +getComponent(Class)
    }

    class Component {
        <<interface>>
    }

    class ComponentStore {
        <<interface>>
        +get(entityId)
        +set(entityId, component)
    }

    class SoAStore {
        <<abstract>>
        #AtomicLongArray data
    }

    class HealthSoAStore {
        +getHealth(entityId)
    }

    class EntitySystem {
        <<abstract>>
        +requiredComponents()
        +writeComponents()
        +process(entity)
    }

    Entity "1" *-- "many" Component
    ComponentStore <|.. SoAStore
    SoAStore <|-- HealthSoAStore
    EntitySystem ..> Component : filters
```

## Жизненный цикл Тика (Scheduling)

```mermaid
sequenceDiagram
    participant GL as GameLoop
    participant PS as PhaseScheduler
    participant PR as PREPARE Tasks
    participant SM as SIMULATION Tasks (Parallel)
    participant PO as POSTPROCESS Tasks

    GL->>PS: executeTick(tickCount)
    PS->>PR: execute(priority order)
    PR-->>PS: done
    PS->>SM: execute(parallel WorkUnits)
    SM-->>PS: done
    PS->>PO: execute(cleanup, stats, snapshot)
    PO-->>PS: done
    PS-->>GL: tick complete
```

## Иерархия сущностей (Nature Plugin)

```mermaid
classDiagram
    class Organism {
        <<interface>>
        +isAlive()
        +die(DeathCause)
    }

    class Animal {
        +SpeciesKey species
        +long energy
    }

    class Biomass {
        +double amount
    }

    Organism <|-- Animal
    Organism <|-- Biomass
    Animal ..> HealthComponent : has
    Animal ..> MovementComponent : has
```

## Карта города (SimCity Plugin)

```mermaid
classDiagram
    class CityMap {
        -CityTile[][] grid
    }

    class CityTile {
        -List~SimEntity~ entities
        -boolean powered
        -boolean watered
        +cleanupDeadEntities()
    }

    class SimEntity {
        +entityId
    }

    CityMap "1" *-- "many" CityTile
    CityTile "1" *-- "many" SimEntity
```

## Spring Boot & React Integration (Phase 4)

```mermaid
graph LR
    subgraph UI_Layer [island-ui (React + Vite)]
        RD[React Dashboard]
        RC[HTML5 Canvas]
        ZS[Zustand Store]
        RD --- ZS
        RD --- RC
    end

    subgraph App_Layer [island-app (Spring Boot)]
        SC[SimulationController]
        SS[SimulationService]
        SB[SimulationBroadcaster]
        JC[SimulationJacksonConfig]
        GE[GlobalExceptionHandler]
    end

    subgraph Core_Layer [island-engine]
        SE[SimulationEngine]
        CX[SimulationContext]
        GL[GameLoop]
        NP[NamedSimulationPlugin]
    end

    RD -- REST API --> SC
    ZS -- STOMP --> SB
    SC -- Lifecycle --> SS
    SS -- Registry --> NP
    SS -- Manages --> SE
    SE -- Produces --> CX
    CX -- Provides --> GL
    GL -- Registers --> SB
    SB -- Serializes --> WS[WorldSnapshot]
    WS -- Broadcast --> ZS
    SC -.-> GE
```
