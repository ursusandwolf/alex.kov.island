package com.island.engine;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
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
        // Заселяем каждый вид ~10% от максимальной популяции
        for (String species : AnimalFactory.getRegisteredSpecies()) {
            AnimalType type = config.getAnimalType(species);
            if (type != null) {
                int maxPerCell = type.getMaxPerCell();
                // Гарантируем в среднем 10% популяции, но не ниже 5% шанса на 1 особь
                double target = maxPerCell * 0.1;
                int count = (int) target;
                double chanceForExtra = target - count;
                
                // Если по расчету должно быть 0, устанавливаем минимальный порог 5%
                if (count == 0) {
                    chanceForExtra = Math.max(chanceForExtra, 0.05);
                }
                
                if (ThreadLocalRandom.current().nextDouble() < chanceForExtra) {
                    count++;
                }
                
                for (int i = 0; i < count; i++) {
                    Animal a = AnimalFactory.createAnimal(species);
                    if (a != null) cell.addAnimal(a);
                }
            }
        }
        
        // Растения - 10% от максимума (200 * 0.1 = 20)
        int plantCount = 20;
        for (int i = 0; i < plantCount; i++) {
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
