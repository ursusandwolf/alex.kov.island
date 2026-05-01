package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.engine.SimulationNode;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.concurrent.atomic.AtomicInteger;

public class PopulationService implements CellService<SimEntity, CityTile> {
    private final CityMap map;
    private final AtomicInteger totalPopulation = new AtomicInteger(0);

    public PopulationService(CityMap map) {
        this.map = map;
    }

    @Override
    public void beforeTick(int tickCount) {
        totalPopulation.set(0);
    }

    @Override
    public void processCell(CityTile tile, int tickCount) {
        int cellPopulation = 0;
        boolean hasResidential = false;
        int neighborIndustrial = 0;
        int neighborCommercial = 0;

        // Calculate environmental factors from neighbors
        for (SimulationNode<SimEntity> neighborNode : tile.getNeighbors()) {
            CityTile neighbor = (CityTile) neighborNode;
            for (SimEntity entity : neighbor.getEntities()) {
                if (entity instanceof Building building) {
                    if (building.getType() == Building.Type.INDUSTRIAL) {
                        neighborIndustrial++;
                    } else if (building.getType() == Building.Type.COMMERCIAL) {
                        neighborCommercial++;
                    }
                }
            }
        }

        if (!tile.isConnected()) {
            for (SimEntity entity : tile.getEntities()) {
                if (entity instanceof Resident resident) {
                    resident.updateHappiness(-30);
                    if (resident.getHappiness() < 10) {
                        resident.die();
                    }
                }
            }
            return;
        }

        for (SimEntity entity : tile.getEntities()) {
            if (entity instanceof Resident resident) {
                cellPopulation++;
                resident.setAge(resident.getAge() + 1);
                
                // Happiness factors
                int happinessDelta = 2; // Base connected bonus
                happinessDelta -= neighborIndustrial * 10; // Pollution penalty
                happinessDelta += neighborCommercial * 15; // Amenities bonus
                
                resident.updateHappiness(happinessDelta);
                
                // Migration out
                if (resident.getHappiness() < 20 && tickCount % 2 == 0) {
                    resident.die(); // Leaves the city
                }
                
                if (resident.getAge() > 100) {
                    resident.die();
                }
            } else if (entity instanceof Building building) {
                if (building.getType() == Building.Type.RESIDENTIAL) {
                    hasResidential = true;
                }
            }
        }

        // Migration in (growth)
        if (hasResidential && cellPopulation < 5) {
            // Check if city is attractive and there is demand
            boolean attractive = tile.getEntities().stream()
                    .filter(e -> e instanceof Resident)
                    .map(e -> (Resident) e)
                    .mapToInt(Resident::getHappiness)
                    .average().orElse(100.0) > 40;
                
            if (attractive && map.getResDemand() > 0) {
                tile.addEntity(new Resident());
            }
        }

        totalPopulation.addAndGet(cellPopulation);
    }

    @Override
    public void afterTick(int tickCount) {
        // Update global map population if needed
    }
}
