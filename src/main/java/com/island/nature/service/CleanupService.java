package com.island.nature.service;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.engine.SimulationNode;
import com.island.nature.model.Cell;
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
    public int priority() {
        return 10;
    }

    @Override
    public void processCell(SimulationNode<Organism> node, int tickCount) {
        if (node instanceof Cell cell) {
            cell.cleanupDeadEntities(e -> {
                if (e instanceof Animal a) {
                    getWorld().onOrganismRemoved(a.getSpeciesKey());
                    animalFactory.releaseAnimal(a);
                }
            });
        }
    }
}
