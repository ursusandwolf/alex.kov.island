package com.island.engine;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.BasicPlant;
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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Заселяем виды вероятностно для создания кластеров и разнообразия
        for (String species : AnimalFactory.getRegisteredSpecies()) {
            AnimalType type = config.getAnimalType(species);
            if (type == null) continue;

            // Вероятность присутствия вида в данной клетке
            // Хищники встречаются реже, травоядные чаще
            double presenceProbability = type.isPredator() ? 0.3 : 0.6;
            
            if (random.nextDouble() < presenceProbability) {
                int maxPerCell = type.getMaxPerCell();
                // Заселяем от 2% до 15% от максимума
                double settlementRate = 0.02 + (random.nextDouble() * 0.13);
                int count = (int) (maxPerCell * settlementRate);
                
                // Гарантируем хотя бы 1 особь если вид "присутствует"
                count = Math.max(count, 1);
                
                for (int i = 0; i < count; i++) {
                    Animal a = AnimalFactory.createAnimal(species);
                    if (a != null) cell.addAnimal(a);
                }
            }
        }
        
        // Растения - присутствуют почти везде (90% клеток)
        if (random.nextDouble() < 0.9) {
            int plantCount = 5 + random.nextInt(30); // 5-35 растений
            for (int i = 0; i < plantCount; i++) {
                cell.addPlant(new BasicPlant());
            }
        }
    }
}
