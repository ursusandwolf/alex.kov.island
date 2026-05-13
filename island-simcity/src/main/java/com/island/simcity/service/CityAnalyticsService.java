package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.scheduling.Phase;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;

public class CityAnalyticsService extends AbstractSimCityService {
    private final CityMap map;

    public CityAnalyticsService(CityMap map) {
        this.map = map;
    }
    private final AtomicInteger pop = new AtomicInteger();
    private final AtomicInteger jobs = new AtomicInteger();
    private final AtomicInteger totalEQ = new AtomicInteger();
    private final AtomicInteger totalHealth = new AtomicInteger();

    @Override
    public Phase phase() {
        return Phase.SIMULATION;
    }

    @Override
    public int priority() {
        return 50; // After Zoning (80)
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(PopulationComponent.class, BuildingComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        pop.set(0);
        jobs.set(0);
        totalEQ.set(0);
        totalHealth.set(0);
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        for (SimEntity entity : tile.getEntities()) {
            PopulationComponent popComp = entity.getComponent(PopulationComponent.class);
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            
            if (popComp != null) {
                pop.incrementAndGet();
                totalEQ.addAndGet(popComp.getEducation());
                totalHealth.addAndGet(popComp.getHealth());
            } else if (building != null) {
                int jobCount = switch (building.getType()) {
                    case INDUSTRIAL -> switch (building.getDensity()) {
                        case LOW -> 10;
                        case MEDIUM -> 50;
                        case HIGH -> 250;
                    };
                    case HIGH_TECH -> switch (building.getDensity()) {
                        case LOW -> 15;
                        case MEDIUM -> 80;
                        case HIGH -> 400;
                    };
                    case COMMERCIAL -> switch (building.getDensity()) {
                        case LOW -> 5;
                        case MEDIUM -> 30;
                        case HIGH -> 150;
                    };
                    default -> 0;
                };
                jobs.addAndGet(jobCount);
            }
        }
    }

    @Override
    public void afterTick(int tickCount) {
        int currentPop = pop.get();
        int currentJobs = jobs.get();
        map.setPopulation(currentPop);
        map.setTotalJobs(currentJobs);
        
        if (currentPop > 0) {
            map.setAverageEQ(totalEQ.get() / currentPop);
            map.setAverageHealth(totalHealth.get() / currentPop);
        }
        
        map.setResDemand(clamp((currentJobs - currentPop) * 5));
        map.setIndDemand(clamp((currentPop - currentJobs + 10) * 2));
        map.setComDemand(clamp(currentPop / 2 - currentJobs / 4));
    }

    private int clamp(int val) {
        return Math.max(-100, Math.min(100, val));
    }
}
