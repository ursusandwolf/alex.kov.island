package com.island.service;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

public class CleanupService extends AbstractService {

    public CleanupService(SimulationWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    protected void processCell(SimulationNode node) {
        if (node instanceof Cell cell) {
            cell.cleanupDeadOrganisms();
        }
    }
}
