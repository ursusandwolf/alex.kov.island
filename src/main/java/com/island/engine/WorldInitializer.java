package com.island.engine;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesConfig;
import com.island.content.Plant;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.model.Cell;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class WorldInitializer {

    public void initialize(Island island, SpeciesConfig config, ExecutorService executor) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    initializeCell(cell, config);
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Инициализация мира была прервана: " + e.getMessage());
        }
    }

    private void initializeCell(Cell cell, SpeciesConfig config) {
        // Заселяем каждый вид с небольшой вероятностью
        for (String species : AnimalFactory.getRegisteredSpecies()) {
            if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                Animal a = AnimalFactory.createAnimal(species);
                if (a != null) cell.addAnimal(a);
            }
        }

        // Растения
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            cell.addPlant(new Plant(1.0, 1.0, 0) {
                @Override public String getTypeName() { return "Plant"; }
                @Override public String getSpeciesKey() { return "plant"; }
                @Override public Plant reproduce() {
                    return (ThreadLocalRandom.current().nextDouble() < 0.1) ? 
                        new Plant(maxBiomass, growthRate, 0) {
                            @Override public String getTypeName() { return "Plant"; }
                            @Override public String getSpeciesKey() { return "plant"; }
                            @Override public Plant reproduce() { return super.reproduce(); }
                        } : null;
                }
            });
        }
    }
}
