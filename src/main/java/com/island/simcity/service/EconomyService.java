package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.engine.SimulationNode;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.concurrent.atomic.AtomicLong;

public class EconomyService implements CellService<SimEntity> {
    private final CityMap map;
    private final AtomicLong tickIncome = new AtomicLong(0);

    public EconomyService(CityMap map) {
        this.map = map;
    }

    @Override
    public void beforeTick(int tickCount) {
        tickIncome.set(0);
    }

    @Override
    public void processCell(SimulationNode<SimEntity> node, int tickCount) {
        CityTile tile = (CityTile) node;
        if (!tile.isConnected()) {
            return;
        }
        
        long cellIncome = 0;
        for (SimEntity entity : node.getEntities()) {
            if (entity instanceof Resident) {
                cellIncome += 10; // Tax per resident
            } else if (entity instanceof Building building) {
                if (building.getType() == Building.Type.COMMERCIAL) {
                    cellIncome += 50; // Commercial income
                } else if (building.getType() == Building.Type.INDUSTRIAL) {
                    cellIncome += 100; // Industrial income
                }
            }
        }
        tickIncome.addAndGet(cellIncome);
    }

    @Override
    public void afterTick(int tickCount) {
        map.addMoney(tickIncome.get());
    }
}
