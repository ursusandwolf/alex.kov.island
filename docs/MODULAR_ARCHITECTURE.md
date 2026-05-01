# Modular Simulation Architecture

## Overview
This document describes the decoupled architecture of the simulation platform. The system is divided into two primary logical layers: the **Generic Engine** (`island-core`) and the **Simulation Plugin** (`island-nature`).

## 1. Generic Engine (`com.island.engine`)
The engine is domain-agnostic and provides the foundational infrastructure for running any spatial-based simulation.

### Core Primitives
*   **`Mortal`**: Interface for any entity that has a lifecycle (can be alive or dead).
*   **`Tickable`**: Interface for components that react to time steps.
*   **`SimulationWorld<T extends Mortal>`**: Manages the spatial structure and global simulation state.
*   **`SimulationNode<T extends Mortal>`**: A container for entities at a specific location.
*   **`GameLoop<T extends Mortal>`**: Orchestrates the execution of tasks in parallel using a thread pool.
*   **`CellService<T extends Mortal>`**: A specialized `Tickable` task that processes each node in the world in parallel.

### Design Principles
*   **Type Safety**: Heavily uses Generics to allow plugins to define their own base entity types (e.g., `Organism`, `Particle`, `Vehicle`).
*   **Zero Domain Knowledge**: The engine contains NO imports from `content` or `service` packages. It doesn't know about animals, plants, or eating.
*   **Parallelism**: Automatically partitions the world into chunks and executes `CellService` tasks across available CPU cores.

## 2. Nature Simulation Plugin (`com.island.content`)
This layer implements the specific logic for the "Island Ecosystem" simulation.

### Domain Entities
*   **`Organism`**: The base class for all entities in the nature simulation.
*   **`Animal` / `Biomass`**: Concrete implementations for fauna and flora.
*   **`SpeciesRegistry`**: Data-driven catalog of species properties.

### Bridging Interfaces
*   **`NatureWorld`**: Extends `SimulationWorld<Organism>` to add nature-specific methods like `reportDeath`, `getStatisticsService`, and `getRegistry`.
*   **`AbstractService`**: Implements `CellService<Organism>` and provides shared logic for nature behaviors (hibernation, protection, LOD sampling).

## 3. Plugin Registration
Plugins are integrated into the engine during the bootstrap phase:
1.  **Instantiate World**: Create a concrete implementation of `SimulationWorld` (e.g., `Island`).
2.  **Initialize Tasks**: Use a `TaskRegistry` to create domain-specific services (`FeedingService`, `MovementService`, etc.).
3.  **Register with GameLoop**: Add services to the `GameLoop` using `addRecurringTask()`.
4.  **Start Loop**: The engine takes over scheduling and execution.

## 4. Extending the Platform
To create a new simulation:
1.  Define a base entity type implementing `Mortal`.
2.  Implement `SimulationWorld` and `SimulationNode` for your spatial model.
3.  Create one or more `CellService` implementations for your logic.
4.  Wire them together in a main entry point using the generic `GameLoop`.
