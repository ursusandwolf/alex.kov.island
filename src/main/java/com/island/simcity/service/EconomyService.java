package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EconomyService implements CellService<SimEntity, CityTile> {
    private final CityMap map;
    private final AtomicLong tickIncome = new AtomicLong();
    private final AtomicLong tickExpenses = new AtomicLong();

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
            if (entity instanceof Building b) {
                cellExpenses += switch (b.getType()) {
                    case ROAD -> 2;
                    case RESIDENTIAL -> 5;
                    case COMMERCIAL -> 20;
                    case INDUSTRIAL -> 50;
                };
                if (tile.isConnected()) {
                    cellIncome += switch (b.getType()) {
                        case COMMERCIAL -> 100;
                        case INDUSTRIAL -> 200;
                        default -> 0;
                    };
                }
            } else if (entity instanceof Resident && tile.isConnected()) {
                cellIncome += map.getTaxRate();
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
