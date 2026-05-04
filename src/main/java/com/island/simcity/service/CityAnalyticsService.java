package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CityAnalyticsService implements CellService<SimEntity, CityTile> {
    private final CityMap map;
    private final AtomicInteger pop = new AtomicInteger();
    private final AtomicInteger jobs = new AtomicInteger();

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
            } else if (entity instanceof Building b) {
                int jobCount = switch (b.getType()) {
                    case INDUSTRIAL -> 10;
                    case COMMERCIAL -> 5;
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
        map.setResDemand(clamp((currentJobs - currentPop) * 5));
        map.setIndDemand(clamp((currentPop - currentJobs + 10) * 2));
        map.setComDemand(clamp(currentPop / 2 - currentJobs / 4));
    }

    private int clamp(int val) {
        return Math.max(-100, Math.min(100, val));
    }
}
