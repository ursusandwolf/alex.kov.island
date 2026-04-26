package com.island.service;

import com.island.content.Animal;
import com.island.content.plants.Plant;
import com.island.model.Cell;
import com.island.model.Island;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class LifecycleService extends AbstractService {

    public LifecycleService(Island island, ExecutorService executor) {
        super(island, executor);
    }

    @Override
    protected void processCell(Cell cell) {
        Island island = cell.getIsland();
        // Process Animals
        List<Animal> animals = cell.getAnimals();
        for (int i = 0; i < animals.size(); i++) {
            Animal animal = animals.get(i);
            if (animal.isAlive()) {
                animal.setHiding(false); // Reset protection flag
                
                // 1. Check Age Death
                if (animal.checkAgeDeath()) {
                    island.reportAgeDeath();
                    continue;
                }

                // 2. Consume Metabolism Energy
                animal.consumeEnergy(animal.getMaxEnergy() * com.island.config.SimulationConstants.BASE_METABOLISM_PERCENT);
                
                // 3. Check Hunger Death
                if (!animal.isAlive() && animal.isStarving()) {
                    island.reportHungerDeath();
                }
            }
        }

        // Process Plants (Growth)
        List<Plant> plants = cell.getPlants();
        for (int i = 0; i < plants.size(); i++) {
            Plant plant = plants.get(i);
            plant.grow();
        }
    }
}
