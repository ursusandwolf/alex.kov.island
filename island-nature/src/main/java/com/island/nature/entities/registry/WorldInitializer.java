package com.island.nature.entities.registry;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;
import com.island.nature.entities.herbivores.Butterfly;
import com.island.nature.entities.herbivores.Caterpillar;
import com.island.nature.entities.plants.Grass;
import com.island.nature.entities.plants.Mushroom;
import com.island.nature.model.Cell;
import com.island.nature.model.Chunk;
import com.island.nature.model.Island;
import com.island.nature.model.TerrainType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.GenericBiomass;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;
import com.island.util.common.RandomProvider;
import java.util.function.Function;
import java.util.Optional;

/**
 * Initializes the island world with organisms using integer-based arithmetic.
 */
public class WorldInitializer {

    private static final int RIVER_WIDTH = 3;
    private static final double RIVER_WINDING_FREQ = 0.5;
    private static final int RIVER_WINDING_AMP = 2;
    
    private static final int TERRAIN_MOUNTAIN_THRESHOLD = 15;
    private static final int TERRAIN_FOREST_THRESHOLD = 40;

    private static final Map<String, BiomassCreator> BIOMASS_CREATORS = Map.of(
        "butterfly", (island, compRegistry, key, amount, capacity, speed) -> 
            new Butterfly(island.getConfiguration(), compRegistry, key, amount, capacity, speed),
        "caterpillar", (island, compRegistry, key, amount, capacity, speed) -> 
            new Caterpillar(island.getConfiguration(), compRegistry, key, amount, capacity, speed),
        "grass", (island, compRegistry, key, amount, capacity, speed) -> {
            Grass g = new Grass(island.getConfiguration(), compRegistry, key, capacity, speed);
            g.setBiomass(amount);
            return g;
        },
        "mushroom", (island, compRegistry, key, amount, capacity, speed) -> {
            Mushroom m = new Mushroom(island.getConfiguration(), compRegistry, key, capacity, speed);
            m.setBiomass(amount);
            return m;
        }
    );

    @FunctionalInterface
    private interface BiomassCreator {
        Biomass create(Island island, ComponentRegistry compRegistry, SpeciesKey key, long amount, long capacity, int speed);
    }

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

    public void initializeFromSnapshot(Island island, SpeciesRegistry registry, AnimalFactory animalFactory, 
                                       WorldSnapshot snapshot, ExecutorService executor, RandomProvider random) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    if (cell.getX() < snapshot.getWidth() && cell.getY() < snapshot.getHeight()) {
                        NodeSnapshot nodeSnapshot = snapshot.getNodeSnapshot(cell.getX(), cell.getY());
                        initializeCellFromSnapshot(cell, nodeSnapshot, registry, animalFactory, random);
                    } else {
                        // Fallback for cells outside the snapshot bounds
                        initializeCell(cell, registry, animalFactory, random);
                    }
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("World snapshot initialization was interrupted: " + e.getMessage());
        }
    }

    private void initializeCell(Cell cell, SpeciesRegistry registry, AnimalFactory animalFactory, RandomProvider random) {
        Island island = (Island) cell.getWorld();
        applyTerrain(cell, island, random);

        ComponentRegistry compRegistry = island.getComponentRegistry();

        // Initialize biomass containers
        for (SpeciesKey biomassKey : registry.getAllBiomassKeys()) {
            registry.getBiomassType(biomassKey).ifPresent(type -> {
                long capacity = type.getWeight() * type.getMaxPerCell();
                long initialAmount = (capacity * type.getPresenceChance()) / 100;
                addBiomass(cell, island, compRegistry, biomassKey, type, initialAmount, capacity);
            });
        }

        // Initialize animal species
        for (SpeciesKey species : animalFactory.getRegisteredSpecies()) {
            registry.getAnimalType(species).ifPresent(type -> {
                if (random.nextInt(0, 100) < type.getPresenceChance()) {
                    long base = type.getSettlementBase() * 100 / island.getConfiguration().getScale1M();
                    long range = type.getSettlementRange() * 100 / island.getConfiguration().getScale1M();
                    int settlementPercent = (int) (base + (range > 0 ? random.nextInt(0, (int) range + 1) : 0));
                    
                    int count = Math.max((type.getMaxPerCell() * settlementPercent) / 100, type.getMaxPerCell() >= 2 ? 2 : 1);
                    
                    for (int i = 0; i < count; i++) {
                        animalFactory.createInitialAnimal(species).ifPresent(a -> cell.addAnimal(a, false));
                    }
                }
            });
        }
    }

    private void initializeCellFromSnapshot(Cell cell, NodeSnapshot snapshot, SpeciesRegistry registry, AnimalFactory animalFactory, RandomProvider random) {
        Island island = (Island) cell.getWorld();
        applyTerrain(cell, island, random);

        ComponentRegistry compRegistry = island.getComponentRegistry();
        Map<String, Integer> counts = snapshot.getEntityCounts();

        // Initialize biomass containers
        for (SpeciesKey biomassKey : registry.getAllBiomassKeys()) {
            registry.getBiomassType(biomassKey).ifPresent(type -> {
                long capacity = type.getWeight() * type.getMaxPerCell();
                int count = counts != null ? counts.getOrDefault(biomassKey.getCode(), 0) : 0;
                long amount = count * type.getWeight();
                addBiomass(cell, island, compRegistry, biomassKey, type, amount, capacity);
            });
        }

        // Initialize animal species
        for (SpeciesKey species : animalFactory.getRegisteredSpecies()) {
            if (counts != null && counts.containsKey(species.getCode())) {
                int count = counts.get(species.getCode());
                for (int i = 0; i < count; i++) {
                    animalFactory.createInitialAnimal(species).ifPresent(a -> cell.addAnimal(a, false));
                }
            }
        }
    }

    private void applyTerrain(Cell cell, Island island, RandomProvider random) {
        int riverCenter = island.getWidth() / 2 + (int)(Math.sin(cell.getY() * RIVER_WINDING_FREQ) * RIVER_WINDING_AMP);
        boolean isRiver = Math.abs(cell.getX() - riverCenter) <= (RIVER_WIDTH / 2);

        if (isRiver) {
            cell.setTerrainType(TerrainType.WATER);
        } else {
            int terrainRoll = random.nextInt(100);
            if (terrainRoll < TERRAIN_MOUNTAIN_THRESHOLD) {
                cell.setTerrainType(TerrainType.MOUNTAIN);
            } else if (terrainRoll < TERRAIN_FOREST_THRESHOLD) {
                cell.setTerrainType(TerrainType.FOREST);
            } else {
                cell.setTerrainType(TerrainType.MEADOW);
            }
        }
    }

    private void addBiomass(Cell cell, Island island, ComponentRegistry compRegistry, SpeciesKey key, 
                            AnimalType type, long amount, long capacity) {
        Biomass b = BIOMASS_CREATORS.getOrDefault(key.getCode(), (isl, reg, k, amt, cap, spd) -> {
            GenericBiomass gb = new GenericBiomass(type, reg);
            gb.setBiomass(amt);
            return gb;
        }).create(island, compRegistry, key, amount, capacity, type.getSpeed());

        cell.addBiomass(b);
        if (cell.getWorld() instanceof NatureWorld nw) {
            nw.getStatisticsService().registerBiomassChange(key, b.getBiomass());
        }
    }
}

