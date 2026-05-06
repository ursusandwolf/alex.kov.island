package com.island.nature.service;

import com.island.nature.model.Cell;
import java.util.concurrent.ExecutorService;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.util.common.RandomProvider;

/**
 * Service responsible for cleaning up dead organisms and returning them to the pool.
 */
public class CleanupService extends AbstractService {
    private final AnimalFactory animalFactory;

    public CleanupService(NatureWorld world, AnimalFactory animalFactory, 
                          ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_CLEANUP;
    }

    @Override
    protected void doProcessCell(Cell cell, int tickCount) {
        cell.cleanupDeadEntities(e -> {
            if (e instanceof Animal a) {
                animalFactory.releaseAnimal(a);
            }
        });
    }
}