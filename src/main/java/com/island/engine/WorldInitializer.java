package com.island.engine;
import com.island.util.RandomUtils;import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.plants.Grass;
import com.island.content.plants.Cabbage;
import com.island.content.animals.herbivores.Caterpillar;
import com.island.content.SpeciesConfig;
import com.island.content.plants.Plant;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.model.Cell;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;


public class WorldInitializer {

    public void initialize(Island island, SpeciesConfig config, AnimalFactory animalFactory, ExecutorService executor) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    initializeCell(cell, config, animalFactory);
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

    private void initializeCell(Cell cell, SpeciesConfig config, AnimalFactory animalFactory) {
         
        
        // Заселяем виды вероятностно для создания кластеров и разнообразия
        for (String species : animalFactory.getRegisteredSpecies()) {
            AnimalType type = config.getAnimalType(species);
            if (type == null) continue;

            // Вероятность присутствия вида в данной клетке
            double presenceProbability = type.isPredator() ? 0.4 : 0.8;
            
            // Специальное снижение для топ-хищников
            if (species.equals("bear")) {
                presenceProbability = 0.15; // 15% cells
            }
            if (species.equals("wolf")) {
                presenceProbability = 0.05; // Only 5% cells (Extreme sparsity for wolves)
            }
            
            if (RandomUtils.nextDouble() < presenceProbability) {
                int maxPerCell = type.getMaxPerCell();
                
                // Заселяем от 10% до 35% от максимума
                double settlementRate = 0.10 + (RandomUtils.nextDouble() * 0.25);
                
                // Для медведей и волков снижаем еще и плотность в ячейке
                if (species.equals("bear")) {
                    settlementRate = 0.05 + (RandomUtils.nextDouble() * 0.05); // 5-10%
                }
                if (species.equals("wolf")) {
                    settlementRate = 0.02 + (RandomUtils.nextDouble() * 0.03); // 2-5% (1-2 wolves max)
                }
                
                int count = (int) (maxPerCell * settlementRate);
                
                // Гарантируем хотя бы 1 особь если вид "присутствует"
                count = Math.max(count, 1);
                
                for (int i = 0; i < count; i++) {
                    Animal a = animalFactory.createAnimal(species);
                    if (a != null) cell.addAnimal(a);
                }
            }
        }
        
        // Растения - присутствуют в каждой клетке (100% шанс)
        cell.addPlant(new Grass());
        cell.addPlant(new Cabbage());
        cell.addPlant(new Caterpillar());
    }
}
