package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ReproductionService implements Runnable {
    private final Island island;

    public ReproductionService(Island island) {
        this.island = island;
    }

    @Override
    public void run() {
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                processCell(island.getCell(x, y));
            }
        }
    }

    private void processCell(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        Map<String, Integer> speciesCounts = new HashMap<>();
        
        for (Animal animal : animals) {
            if (!animal.isAlive()) continue;
            speciesCounts.put(animal.getSpeciesKey(), speciesCounts.getOrDefault(animal.getSpeciesKey(), 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : speciesCounts.entrySet()) {
            String speciesKey = entry.getKey();
            int count = entry.getValue();
            
            // Если есть пара (минимум 2 животных одного вида)
            if (count >= 2) {
                // Создаем новое животное того же вида
                Animal baby = AnimalFactory.createAnimal(speciesKey);
                if (baby != null) {
                    cell.addAnimal(baby);
                }
            }
        }
    }
}
