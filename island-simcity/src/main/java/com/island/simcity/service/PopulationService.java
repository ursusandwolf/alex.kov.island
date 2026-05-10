package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.island.engine.core.SimulationNode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PopulationService extends AbstractSimCityService {
    
    private static final int PENALTY_UNCONNECTED = -30;
    private static final int DEATH_THRESHOLD = 10;
    private static final int LEAVE_CITY_THRESHOLD = 20;
    private static final int MAX_AGE = 100;
    private static final int MAX_CELL_POPULATION = 5;
    private static final int MIN_ATTRACTIVENESS = 40;
    
    private static final int BONUS_CONNECTED = 2;
    private static final int PENALTY_NEIGHBOR_IND = 10;
    private static final int BONUS_NEIGHBOR_COM = 15;
    private static final int BONUS_NEIGHBOR_AGR = 5;
    
    private static final int BONUS_WATER = 5;
    private static final int PENALTY_NO_WATER = 20;
    private static final int BONUS_POWER = 10;
    private static final int PENALTY_NO_POWER = 30;
    private static final int BONUS_RAIL = 10;
    private static final int BONUS_METRO = 20;
    
    private static final int AIR_POLLUTION_THRESHOLD = 50;
    private static final int AIR_POLLUTION_DIVISOR = 10;
    private static final int WATER_POLLUTION_THRESHOLD = 30;
    private static final int WATER_POLLUTION_DIVISOR = 5;
    
    private static final int BASE_TAX_RATE = 20;
    private static final int PENALTY_BANKRUPTCY = 20;

    private final CityMap map;
    private final ComponentRegistry registry;
    private final AtomicInteger totalPopulation = new AtomicInteger(0);

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
        totalPopulation.set(0);
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        if (!tile.isConnected()) {
            processUnconnectedTile(tile);
            return;
        }

        EnvironmentFactors env = calculateEnvironmentFactors(tile);
        TileState state = processEntities(tile, tickCount, env);
        processMigration(tile, state);

        totalPopulation.addAndGet(state.cellPopulation);
    }

    private void processUnconnectedTile(CityTile tile) {
        int cellPop = 0;
        for (SimEntity entity : tile.getEntities()) {
            PopulationComponent pop = entity.getComponent(PopulationComponent.class);
            if (pop != null) {
                cellPop++;
                pop.updateHappiness(PENALTY_UNCONNECTED);
                if (pop.getHappiness() < DEATH_THRESHOLD) {
                    entity.die();
                }
            }
        }
        totalPopulation.addAndGet(cellPop);
    }

    private EnvironmentFactors calculateEnvironmentFactors(CityTile tile) {
        EnvironmentFactors env = new EnvironmentFactors();
        for (SimulationNode<SimEntity> neighborNode : tile.getNeighbors()) {
            CityTile neighbor = (CityTile) neighborNode;
            for (SimEntity entity : neighbor.getEntities()) {
                BuildingComponent building = entity.getComponent(BuildingComponent.class);
                if (building != null) {
                    if (building.getType() == BuildingComponent.Type.INDUSTRIAL) env.neighborIndustrial++;
                    else if (building.getType() == BuildingComponent.Type.COMMERCIAL) env.neighborCommercial++;
                    else if (building.getType() == BuildingComponent.Type.AGRICULTURAL) env.neighborAgricultural++;
                }
            }
        }
        return env;
    }

    private TileState processEntities(CityTile tile, int tickCount, EnvironmentFactors env) {
        TileState state = new TileState();
        int baseHappinessDelta = calculateBaseHappinessDelta(tile, env);

        for (SimEntity entity : tile.getEntities()) {
            PopulationComponent pop = entity.getComponent(PopulationComponent.class);
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            
            if (pop != null) {
                state.cellPopulation++;
                pop.setAge(pop.getAge() + 1);
                
                pop.updateHappiness(baseHappinessDelta);
                
                if (pop.getHappiness() < LEAVE_CITY_THRESHOLD && tickCount % 2 == 0) {
                    entity.die(); // Leaves the city
                    map.addAlert("Residents leaving: Low Happiness");
                }
                
                if (pop.getAge() > MAX_AGE) {
                    entity.die();
                }
            } else if (building != null && building.getType() == BuildingComponent.Type.RESIDENTIAL) {
                state.hasResidential = true;
            }
        }
        return state;
    }

    private int calculateBaseHappinessDelta(CityTile tile, EnvironmentFactors env) {
        int delta = BONUS_CONNECTED;
        delta -= env.neighborIndustrial * PENALTY_NEIGHBOR_IND;
        delta += env.neighborCommercial * BONUS_NEIGHBOR_COM;
        delta += env.neighborAgricultural * BONUS_NEIGHBOR_AGR;
        
        delta += tile.isWatered() ? BONUS_WATER : -PENALTY_NO_WATER;
        delta += tile.isPowered() ? BONUS_POWER : -PENALTY_NO_POWER;
        
        if (tile.isRailed()) delta += BONUS_RAIL;
        if (tile.isMetroConnected()) delta += BONUS_METRO;
        
        if (tile.getAirPollution() > AIR_POLLUTION_THRESHOLD) {
            delta -= (tile.getAirPollution() - AIR_POLLUTION_THRESHOLD) / AIR_POLLUTION_DIVISOR;
        }
        if (tile.getWaterPollution() > WATER_POLLUTION_THRESHOLD) {
            delta -= (tile.getWaterPollution() - WATER_POLLUTION_THRESHOLD) / WATER_POLLUTION_DIVISOR;
        }
        
        if (map.getTaxRate() > BASE_TAX_RATE) {
            delta -= (map.getTaxRate() - BASE_TAX_RATE);
        }
        
        if (map.isBankrupt()) {
            delta -= PENALTY_BANKRUPTCY;
        }
        
        return delta;
    }

    private void processMigration(CityTile tile, TileState state) {
        if (!state.hasResidential || state.cellPopulation >= MAX_CELL_POPULATION) {
            return;
        }

        if (map.isBankrupt()) {
            map.addAlert("No growth: Bankruptcy");
            return;
        }

        if (isTileAttractive(tile) && map.getResDemand() > 0) {
            SimEntity resident = new SimEntity(registry);
            resident.addComponent(PopulationComponent.builder().age(0).happiness(100).build());
            tile.addEntity(resident);
        }
    }

    private boolean isTileAttractive(CityTile tile) {
        int totalHappiness = 0;
        int count = 0;
        for (SimEntity e : tile.getEntities()) {
            PopulationComponent pop = e.getComponent(PopulationComponent.class);
            if (pop != null) {
                totalHappiness += pop.getHappiness();
                count++;
            }
        }
        return count == 0 || (totalHappiness / count) > MIN_ATTRACTIVENESS;
    }

    @Override
    public void afterTick(int tickCount) {
        map.setPopulation(totalPopulation.get());
    }

    private static class EnvironmentFactors {
        int neighborIndustrial = 0;
        int neighborCommercial = 0;
        int neighborAgricultural = 0;
    }

    private static class TileState {
        int cellPopulation = 0;
        boolean hasResidential = false;
    }
}
