package com.island.nature.entities;

import com.island.nature.entities.herbivores.Butterfly;
import com.island.nature.entities.herbivores.Caterpillar;
import com.island.nature.entities.plants.Grass;
import com.island.nature.entities.plants.Mushroom;
import com.island.engine.SimulationNode;
import com.island.nature.model.Cell;
import com.island.nature.model.Chunk;
import com.island.nature.model.Island;
import com.island.nature.model.TerrainType;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Initializes the island world with organisms using integer-based arithmetic.
 */
public class WorldInitializer {

    public void initialize(Island island, SpeciesRegistry registry, AnimalFactory animalFactory, 
                           ExecutorService executor, RandomProvider random) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    initializeCell(cell, registry, animalFactory, random);
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("World initialization was interrupted: " + e.getMessage());
        }
    }

    private void initializeCell(Cell cell, SpeciesRegistry registry, AnimalFactory animalFactory, RandomProvider random) {
        // Randomly assign terrain type
        int terrainRoll = random.nextInt(100);
        if (terrainRoll < 10) {
            cell.setTerrainType(TerrainType.WATER);
        } else if (terrainRoll < 20) {
            cell.setTerrainType(TerrainType.MOUNTAIN);
        } else if (terrainRoll < 40) {
            cell.setTerrainType(TerrainType.FOREST);
        } else {
            cell.setTerrainType(TerrainType.MEADOW);
        }

        Island island = (Island) cell.getWorld();

        // Initialize biomass containers (Plants, Insects modeled as biomass)
        for (SpeciesKey biomassKey : registry.getAllBiomassKeys()) {
            registry.getBiomassType(biomassKey).ifPresent(type -> {
                Biomass b;
                if (biomassKey == SpeciesKey.BUTTERFLY) {
                    b = new Butterfly(island.getConfiguration(), type.getMaxEnergy() * type.getMaxPerCell(), type.getSpeed());
                } else if (biomassKey == SpeciesKey.CATERPILLAR) {
                    b = new Caterpillar(island.getConfiguration(), type.getMaxEnergy() * type.getMaxPerCell(), type.getSpeed());
                } else if (biomassKey == SpeciesKey.GRASS) {
                    b = new Grass(island.getConfiguration(), type.getMaxEnergy() * type.getMaxPerCell(), type.getSpeed());
                } else if (biomassKey == SpeciesKey.MUSHROOM) {
                    b = new Mushroom(island.getConfiguration(), type.getMaxEnergy() * type.getMaxPerCell(), type.getSpeed());
                } else {
                    b = new GenericBiomass(type);
                }
                cell.addBiomass(b);
                if (cell.getWorld() instanceof NatureWorld nw) {
                    nw.getStatisticsService().registerBiomassChange(biomassKey, b.getBiomass());
                }
            });
        }

        // Initialize animal species
        for (SpeciesKey species : animalFactory.getRegisteredSpecies()) {
            AnimalType type = registry.getAnimalType(species).orElse(null);
            if (type == null) {
                continue;
            }

            // Data-driven settlement for animals (0-100)
            if (random.nextInt(0, 100) < type.getPresenceChance()) {
                // settlementRate in percent
                long base = type.getSettlementBase() * 100 / island.getConfiguration().getScale1M();
                long range = type.getSettlementRange() * 100 / island.getConfiguration().getScale1M();
                int settlementPercent = (int) (base + (range > 0 ? random.nextInt(0, (int) range + 1) : 0));
                
                int count = (type.getMaxPerCell() * settlementPercent) / 100;                
                count = Math.max(count, 1);
                
                for (int i = 0; i < count; i++) {
                    animalFactory.createInitialAnimal(species).ifPresent(cell::addAnimal);
                }
            }
        }
    }
}
