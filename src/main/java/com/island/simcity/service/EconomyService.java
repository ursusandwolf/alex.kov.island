package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.engine.SimulationNode;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.concurrent.atomic.AtomicLong;

public class EconomyService implements CellService<SimEntity, CityTile> {
    private final CityMap map;
    private final AtomicLong tickIncome = new AtomicLong(0);
    private final AtomicLong tickExpenses = new AtomicLong(0);

    public EconomyService(CityMap map) {
        this.map = map;
    }

    @Override
    public void beforeTick(int tickCount) {
        tickIncome.set(0);
        tickExpenses.set(0);
    }

    @Override
    public void processCell(CityTile tile, int tickCount) {
        long cellIncome = 0;
        long cellExpenses = 0;

        for (SimEntity entity : tile.getEntities()) {
            if (entity instanceof Building building) {
                // Maintenance costs (always applied if building exists)
                cellExpenses += switch (building.getType()) {
                    case ROAD -> 2;
                    case RESIDENTIAL -> 5;
                    case COMMERCIAL -> 20;
                    case INDUSTRIAL -> 50;
                };

                // Income (only if connected)
                if (tile.isConnected()) {
                    if (building.getType() == Building.Type.COMMERCIAL) {
                        cellIncome += 100;
                    } else if (building.getType() == Building.Type.INDUSTRIAL) {
                        cellIncome += 200;
                    }
                }
            } else if (entity instanceof Resident && tile.isConnected()) {
                cellIncome += 15; // Tax per resident
            }
        }
        
        tickIncome.addAndGet(cellIncome);
        tickExpenses.addAndGet(cellExpenses);
    }

    @Override
    public void afterTick(int tickCount) {
        map.setLastTickIncome(tickIncome.get());
        map.setLastTickExpenses(tickExpenses.get());
        map.addMoney(tickIncome.get() - tickExpenses.get());
    }
}
