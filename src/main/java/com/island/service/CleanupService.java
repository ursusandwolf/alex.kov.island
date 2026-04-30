package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for cleaning up dead organisms and returning them to the pool.
 */
public class CleanupService extends AbstractService<SimulationNode> {
    private final AnimalFactory animalFactory;

    public CleanupService(SimulationWorld world, AnimalFactory animalFactory, 
                          ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
    }

    @Override
    public void processCell(SimulationNode node, int tickCount) {
        if (node instanceof Cell cell) {
            cell.getLock().lock();
            try {
                cell.getContainer().removeDeadAnimals(a -> {
                    getWorld().onOrganismRemoved(a.getSpeciesKey());
                    animalFactory.releaseAnimal(a);
                });
            } finally {
                cell.getLock().unlock();
            }
        }
    }
}
