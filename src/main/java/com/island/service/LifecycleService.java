package com.island.service;

import com.island.content.Animal;
import com.island.content.DeathCause;
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
                    island.reportDeath(animal.getSpeciesKey(), DeathCause.AGE);
                    continue;
                }

                // 2. Consume Metabolism Energy
                if (!animal.isHibernating()) {
                    if (!animal.tryConsumeEnergy(animal.getMaxEnergy() * animal.getDynamicMetabolismRate())) {
                        island.reportDeath(animal.getSpeciesKey(), DeathCause.HUNGER);
                    }
                }
            }
        }

        // Process Plants (Growth & Pendulum)
        List<Plant> plants = cell.getPlants();
        for (int i = 0; i < plants.size(); i++) {
            Plant plant = plants.get(i);
            plant.setHiding(false); 
            
            if (plant instanceof com.island.content.animals.herbivores.Caterpillar caterpillar) {
                caterpillar.processPendulum(cell);
            } else {
                plant.grow();
            }
        }
    }
}
