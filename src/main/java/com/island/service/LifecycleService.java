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
        // Process Animals
        List<Animal> animals = cell.getAnimals();
        for (Animal animal : animals) {
            if (animal.isAlive()) {
                animal.setHiding(false); // Reset protection flag
                animal.checkState();
            }
        }

        // Process Plants (Growth and potentially death if biomass=0)
        List<Plant> plants = cell.getPlants();
        for (Plant plant : plants) {
            if (plant.isAlive()) {
                plant.checkState();
            }
        }
    }
}
