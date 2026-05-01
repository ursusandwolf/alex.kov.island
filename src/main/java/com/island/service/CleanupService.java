package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.NatureWorld;
import com.island.content.Organism;
import com.island.engine.SimulationNode;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

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
    public void processCell(Cell cell, int tickCount) {
        cell.cleanupDeadEntities(e -> {
            if (e instanceof Animal a) {
                getWorld().onOrganismRemoved(a.getSpeciesKey());
                animalFactory.releaseAnimal(a);
            }
        });
    }
}
