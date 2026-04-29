package com.island.service;

import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

public class CleanupService extends AbstractService {

    public CleanupService(Island island, ExecutorService executor, RandomProvider random) {
        super(island, executor, random);
    }

    @Override
    protected void processCell(Cell cell) {
        cell.cleanupDeadOrganisms();
    }
}
