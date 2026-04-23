package com.island.service;

import com.island.content.Animal;
import com.island.content.Organism;
import com.island.content.Plant;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.engine.InteractionMatrix;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FeedingService implements Runnable {
    private final Island island;
    private final InteractionMatrix interactionMatrix;

    public FeedingService(Island island, InteractionMatrix interactionMatrix) {
        this.island = island;
        this.interactionMatrix = interactionMatrix;
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
        for (Animal animal : animals) {
            if (!animal.isAlive()) continue;
            // Логика поиска еды и поедания
            tryEat(animal, cell);
        }
    }

    private void tryEat(Animal predator, Cell cell) {
        // Сначала пробуем есть других животных
        List<Animal> potentialPrey = cell.getAnimals();
        for (Animal prey : potentialPrey) {
            if (predator == prey || !prey.isAlive()) continue;
            
            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
            if (ThreadLocalRandom.current().nextInt(100) < chance) {
                // Успешная охота
                predator.addEnergy(prey.getWeight());
                cell.removeAnimal(prey);
                return;
            }
        }

        // Если не поели животных, пробуем растения (для травоядных)
        List<Plant> plants = cell.getPlants();
        for (Plant plant : plants) {
            if (!plant.isAlive()) continue;
            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), "Plant");
            if (ThreadLocalRandom.current().nextInt(100) < chance) {
                // Животное ест растение (по умолчанию 1кг)
                predator.addEnergy(1.0); 
                cell.removePlant(plant);
                return;
            }
        }
    }
}
