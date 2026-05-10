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
        
        // TODO: Migrate magic numbers to Configuration or BuildingProfile enum
        for (SimEntity entity : tile.getEntities()) {
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            PopulationComponent pop = entity.getComponent(PopulationComponent.class);
            
            if (building != null) {
                long densityMultiplier = switch (building.getDensity()) {
                    case LOW -> 1;
                    case MEDIUM -> 4;
                    case HIGH -> 12;
                };

                cellExpenses += (switch (building.getType()) {
                    case ROAD -> 1;
                    case RESIDENTIAL -> 2;
                    case COMMERCIAL -> 5;
                    case INDUSTRIAL -> 10;
                    case AGRICULTURAL -> 1;
                    case RAILWAY -> 5;
                    case METRO -> 20;
                    case WATER_PIPE -> 1;
                    case POWER_PLANT -> 50;
                    case POWER_LINE -> 2;
                }) * densityMultiplier;
                
                if (tile.isConnected()) {
                    long incomePowerPenalty = tile.isPowered() ? 1 : 2; // Full income if powered, half if not
                    cellIncome += (switch (building.getType()) {
                        case COMMERCIAL -> 500;
                        case INDUSTRIAL -> 1000;
                        case AGRICULTURAL -> 100;
                        default -> 0;
                    } * densityMultiplier) / incomePowerPenalty;
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
