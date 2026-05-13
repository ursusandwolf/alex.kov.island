package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.core.SimulationNode;
import com.island.engine.scheduling.Phase;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;

/**
 * Service managing education and health systems.
 * Provides area-of-effect bonuses from social buildings.
 */
public class SocialService extends AbstractSimCityService {
    private final CityMap map;

    public SocialService(CityMap map) {
        this.map = map;
    }

    @Override
    public Phase phase() {
        return Phase.PREPARE;
    }

    @Override
    public int priority() {
        return 35; // Runs after PollutionService (40)
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class);
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        return List.of(PopulationComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        // Reset and natural decay of service coverage
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                CityTile tile = map.getGrid()[x][y];
                tile.setEducationLevel(0);
                tile.setHealthLevel(0);
            }
        }
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        for (SimEntity entity : tile.getEntities()) {
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            if (building != null) {
                applySocialEffects(tile, building);
            }
            
            PopulationComponent pop = entity.getComponent(PopulationComponent.class);
            if (pop != null) {
                updateResidentStats(tile, pop);
            }
        }
    }

    private void applySocialEffects(CityTile tile, BuildingComponent building) {
        switch (building.getType()) {
            case SCHOOL -> {
                tile.setEducationLevel(tile.getEducationLevel() + 60);
                spreadEffect(tile, 1, 30, true);
            }
            case COLLEGE -> {
                tile.setEducationLevel(tile.getEducationLevel() + 100);
                spreadEffect(tile, 2, 50, true);
            }
            case HOSPITAL -> {
                tile.setHealthLevel(tile.getHealthLevel() + 80);
                spreadEffect(tile, 2, 40, false);
            }
            case PARK -> {
                tile.setHealthLevel(tile.getHealthLevel() + 10);
                tile.setDesirability(tile.getDesirability() + 15);
            }
        }
    }

    private void spreadEffect(CityTile center, int radius, int power, boolean education) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                int nx = center.getX() + dx;
                int ny = center.getY() + dy;
                
                if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                    CityTile neighbor = map.getGrid()[nx][ny];
                    int dist = Math.abs(dx) + Math.abs(dy);
                    int attenuatedPower = power / dist;
                    
                    if (education) {
                        neighbor.setEducationLevel(neighbor.getEducationLevel() + attenuatedPower);
                    } else {
                        neighbor.setHealthLevel(neighbor.getHealthLevel() + attenuatedPower);
                    }
                }
            }
        }
    }

    private void updateResidentStats(CityTile tile, PopulationComponent pop) {
        // Education Quotient accumulation
        if (tile.getEducationLevel() > 0) {
            pop.setEducation(Math.min(200, pop.getEducation() + 1 + tile.getEducationLevel() / 20));
        }

        // Health accumulation and pollution impact
        int healthDelta = tile.getHealthLevel() / 10;
        if (tile.getAirPollution() > 40) healthDelta -= (tile.getAirPollution() - 40) / 10;
        if (tile.getWaterPollution() > 20) healthDelta -= (tile.getWaterPollution() - 20) / 5;
        
        pop.setHealth(Math.max(0, Math.min(100, pop.getHealth() + healthDelta)));

        // Bonus happiness from social services
        if (pop.getHealth() > 80) pop.updateHappiness(2);
        if (pop.getEducation() > 100) pop.updateHappiness(1);
    }
}
