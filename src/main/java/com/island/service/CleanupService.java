package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for cleaning up dead organisms and returning them to the pool.
 */
public class CleanupService extends AbstractService<Cell> {
    private final AnimalFactory animalFactory;

    public CleanupService(SimulationWorld world, AnimalFactory animalFactory, 
                          ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
    }

    @Override
    protected void processCell(Cell cell, int tickCount) {
        List<Animal> deadAnimals = cell.cleanupDeadOrganisms();
        for (Animal a : deadAnimals) {
            animalFactory.releaseAnimal(a);
        }
    }
}
