package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EconomyService extends AbstractSimCityService {
    private final CityMap map;
    private final AtomicLong tickIncome = new AtomicLong();
    private final AtomicLong tickExpenses = new AtomicLong();

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class, PopulationComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        tickIncome.set(0);
        tickExpenses.set(0);
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        long cellIncome = 0;
        long cellExpenses = 0;
        for (SimEntity entity : tile.getEntities()) {
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            PopulationComponent pop = entity.getComponent(PopulationComponent.class);
            
            if (building != null) {
                cellExpenses += switch (building.getType()) {
                    case ROAD -> 2;
                    case RESIDENTIAL -> 5;
                    case COMMERCIAL -> 20;
                    case INDUSTRIAL -> 50;
                };
                if (tile.isConnected()) {
                    cellIncome += switch (building.getType()) {
                        case COMMERCIAL -> 100;
                        case INDUSTRIAL -> 200;
                        default -> 0;
                    };
                }
            } else if (pop != null && tile.isConnected()) {
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
