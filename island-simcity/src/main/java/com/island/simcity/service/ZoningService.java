package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityTile;
import java.util.List;

/**
 * Service that manages building density upgrades and resident wealth progression.
 * Buildings evolve based on desirability, utility access, and happiness.
 */
public class ZoningService extends AbstractSimCityService {
    
    private static final int UPGRADE_TICK_INTERVAL = 5;
    private static final int MIN_DESIRABILITY_MEDIUM = 60;
    private static final int MIN_DESIRABILITY_HIGH = 85;
    private static final int MIN_HAPPINESS_MIDDLE = 70;
    private static final int MIN_HAPPINESS_WEALTHY = 90;
    private static final int MAX_POLLUTION_MIDDLE = 20;
    private static final int MAX_POLLUTION_WEALTHY = 5;

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class, PopulationComponent.class);
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        return List.of(BuildingComponent.class, PopulationComponent.class);
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        if (tickCount % UPGRADE_TICK_INTERVAL != 0) {
            return;
        }

        for (SimEntity entity : tile.getEntities()) {
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            if (building == null) continue;

            if (isRCI(building.getType())) {
                updateDensity(tile, building);
                updateWealth(tile, entity);
            }
        }
    }

    private boolean isRCI(BuildingComponent.Type type) {
        return type == BuildingComponent.Type.RESIDENTIAL || 
               type == BuildingComponent.Type.COMMERCIAL || 
               type == BuildingComponent.Type.INDUSTRIAL;
    }

    private void updateDensity(CityTile tile, BuildingComponent building) {
        int desirability = tile.getDesirability();
        boolean hasUtilities = tile.isWatered() && tile.isPowered();

        if (building.getDensity() == BuildingComponent.Density.LOW) {
            if (hasUtilities && desirability >= MIN_DESIRABILITY_MEDIUM) {
                building.setDensity(BuildingComponent.Density.MEDIUM);
            }
        } else if (building.getDensity() == BuildingComponent.Density.MEDIUM) {
            if (hasUtilities && desirability >= MIN_DESIRABILITY_HIGH && tile.isMetroConnected()) {
                building.setDensity(BuildingComponent.Density.HIGH);
            }
        }
    }

    private void updateWealth(CityTile tile, SimEntity entity) {
        PopulationComponent pop = entity.getComponent(PopulationComponent.class);
        if (pop == null) return;

        int happiness = pop.getHappiness();
        int pollution = tile.getAirPollution() + tile.getWaterPollution();

        if (pop.getWealth() == PopulationComponent.WealthLevel.POOR) {
            if (happiness >= MIN_HAPPINESS_MIDDLE && pollution <= MAX_POLLUTION_MIDDLE) {
                pop.setWealth(PopulationComponent.WealthLevel.MIDDLE);
            }
        } else if (pop.getWealth() == PopulationComponent.WealthLevel.MIDDLE) {
            if (happiness >= MIN_HAPPINESS_WEALTHY && pollution <= MAX_POLLUTION_WEALTHY) {
                pop.setWealth(PopulationComponent.WealthLevel.WEALTHY);
            }
        }
    }
}
