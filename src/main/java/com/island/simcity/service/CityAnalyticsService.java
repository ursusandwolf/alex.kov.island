package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.engine.SimulationNode;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.concurrent.atomic.AtomicInteger;

public class CityAnalyticsService implements CellService<SimEntity, CityTile> {
    private final CityMap map;
    private final AtomicInteger pop = new AtomicInteger(0);
    private final AtomicInteger jobs = new AtomicInteger(0);

    public CityAnalyticsService(CityMap map) {
        this.map = map;
    }

    @Override
    public void beforeTick(int tickCount) {
        pop.set(0);
        jobs.set(0);
    }

    @Override
    public void processCell(CityTile node, int tickCount) {
        for (SimEntity entity : node.getEntities()) {
            if (entity instanceof Resident) {
                pop.incrementAndGet();
            } else if (entity instanceof Building building) {
                if (building.getType() == Building.Type.INDUSTRIAL) {
                    jobs.addAndGet(10); // Each factory provides 10 jobs
                } else if (building.getType() == Building.Type.COMMERCIAL) {
                    jobs.addAndGet(5); // Each shop provides 5 jobs
                }
            }
        }
    }

    @Override
    public void afterTick(int tickCount) {
        int currentPop = pop.get();
        int currentJobs = jobs.get();
        
        map.setPopulation(currentPop);
        map.setTotalJobs(currentJobs);
        
        // Calculate Demand
        // Residential demand: more jobs than people
        map.setResDemand(Math.max(-100, Math.min(100, (currentJobs - currentPop) * 5)));
        
        // Industrial demand: more people than jobs
        map.setIndDemand(Math.max(-100, Math.min(100, (currentPop - currentJobs + 10) * 2)));
        
        // Commercial demand: balance
        map.setComDemand(Math.max(-100, Math.min(100, currentPop / 2 - currentJobs / 4)));
    }
}
