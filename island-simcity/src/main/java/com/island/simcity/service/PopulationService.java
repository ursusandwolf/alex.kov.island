package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import com.island.engine.core.SimulationNode;

public class PopulationService extends AbstractSimCityService {
    private final CityMap map;
    private final ComponentRegistry registry;
    private final AtomicInteger totalPopulation = new AtomicInteger(0);

    public PopulationService(CityMap map, ComponentRegistry registry) {
        this.map = map;
        this.registry = registry;
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
        totalPopulation.set(0);
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        int cellPopulation = 0;
        boolean hasResidential = false;
        int neighborIndustrial = 0;
        int neighborCommercial = 0;
        int neighborAgricultural = 0;

        // Calculate environmental factors from neighbors
        for (SimulationNode<SimEntity> neighborNode : tile.getNeighbors()) {
            CityTile neighbor = (CityTile) neighborNode;
            for (SimEntity entity : neighbor.getEntities()) {
                BuildingComponent building = entity.getComponent(BuildingComponent.class);
                if (building != null) {
                    if (building.getType() == BuildingComponent.Type.INDUSTRIAL) {
                        neighborIndustrial++;
                    } else if (building.getType() == BuildingComponent.Type.COMMERCIAL) {
                        neighborCommercial++;
                    } else if (building.getType() == BuildingComponent.Type.AGRICULTURAL) {
                        neighborAgricultural++;
                    }
                }
            }
        }

        if (!tile.isConnected()) {
            for (SimEntity entity : tile.getEntities()) {
                PopulationComponent pop = entity.getComponent(PopulationComponent.class);
                if (pop != null) {
                    pop.updateHappiness(-30);
                    if (pop.getHappiness() < 10) {
                        entity.die();
                    }
                }
            }
            totalPopulation.addAndGet(cellPopulation);
            return;
        }

        for (SimEntity entity : tile.getEntities()) {
            PopulationComponent pop = entity.getComponent(PopulationComponent.class);
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            
            if (pop != null) {
                cellPopulation++;
                pop.setAge(pop.getAge() + 1);
                
                // Happiness factors
                int happinessDelta = 2; // Base connected bonus
                happinessDelta -= neighborIndustrial * 10; // Pollution penalty
                happinessDelta += neighborCommercial * 15; // Amenities bonus
                happinessDelta += neighborAgricultural * 5; // Nature bonus
                
                // New infrastructure bonuses
                if (tile.isWatered()) happinessDelta += 5;
                else happinessDelta -= 20; // Residents hate no water
                
                if (tile.isPowered()) happinessDelta += 10;
                else happinessDelta -= 30; // Residents REALLY hate no power
                
                if (tile.isRailed()) happinessDelta += 10;
                if (tile.isMetroConnected()) happinessDelta += 20;
                
                // Pollution impact
                if (tile.getAirPollution() > 50) {
                    happinessDelta -= (tile.getAirPollution() - 50) / 10;
                }
                if (tile.getWaterPollution() > 30) {
                    happinessDelta -= (tile.getWaterPollution() - 30) / 5;
                }
                
                // Tax penalty
                if (map.getTaxRate() > 20) {
                    happinessDelta -= (map.getTaxRate() - 20);
                }
                
                // Bankruptcy penalty
                if (map.isBankrupt()) {
                    happinessDelta -= 20;
                }

                pop.updateHappiness(happinessDelta);
                
                // Migration out
                if (pop.getHappiness() < 20 && tickCount % 2 == 0) {
                    entity.die(); // Leaves the city
                    map.addAlert("Residents leaving: Low Happiness");
                }
                
                if (pop.getAge() > 100) {
                    entity.die();
                }
            } else if (building != null) {
                if (building.getType() == BuildingComponent.Type.RESIDENTIAL) {
                    hasResidential = true;
                }
            }
        }

        // Migration in (growth)
        if (hasResidential && cellPopulation < 5 && !map.isBankrupt()) {
            // Check if city is attractive and there is demand
            boolean attractive = tile.getEntities().stream()
                    .map(e -> e.getComponent(PopulationComponent.class))
                    .filter(Objects::nonNull)
                    .mapToInt(PopulationComponent::getHappiness)
                    .average().orElse(100.0) > 40;
                
            if (attractive && map.getResDemand() > 0) {
                SimEntity resident = new SimEntity(registry);
                resident.addComponent(PopulationComponent.builder().age(0).happiness(100).build());
                tile.addEntity(resident);
            }
        } else if (hasResidential && cellPopulation < 5 && map.isBankrupt()) {
            map.addAlert("No growth: Bankruptcy");
        }

        totalPopulation.addAndGet(cellPopulation);
    }

    @Override
    public void afterTick(int tickCount) {
        map.setPopulation(totalPopulation.get());
    }
}
