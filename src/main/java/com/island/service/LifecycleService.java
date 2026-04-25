package com.island.service;

import com.island.content.Animal;
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
        List<Animal> animals = cell.getAnimals();
        for (Animal animal : animals) {
            if (animal.isAlive()) {
                animal.setHiding(false); // Reset protection flag
                animal.checkState();
            }
        }
    }
}
