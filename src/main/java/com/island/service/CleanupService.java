package com.island.service;

import com.island.model.Cell;
import com.island.model.Island;
import java.util.concurrent.ExecutorService;

public class CleanupService extends AbstractService {

    public CleanupService(Island island, ExecutorService executor) {
        super(island, executor);
    }

    @Override
    protected void processCell(Cell cell) {
        cell.cleanupDeadOrganisms();
    }
}
