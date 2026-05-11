package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.Entity;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;

import com.island.simcity.entities.SimEntity;

/**
 * ECS System for managing city population dynamics.
 */
public class PopulationSystem extends AbstractSimCitySystem {

    private final CityMap map;

    public PopulationSystem(CityMap map) {
        super(List.of(PopulationComponent.class));
        this.map = map;
        this.entityQuery.bind(map.getComponentRegistry());
    }

    @Override
    public void process(SimEntity entity, CityTile tile, int tickCount) {
        PopulationComponent pop = entity.getComponent(PopulationComponent.class);
        if (pop == null) return;

        pop.setAge(pop.getAge() + 1);
        
        // Happiness delta logic
        int happinessDelta = 2; // Base connected bonus
        
        if (map.getTaxRate() > 20) {
            happinessDelta -= (map.getTaxRate() - 20);
        }
        
        if (map.isBankrupt()) {
            happinessDelta -= 20;
        }

        pop.updateHappiness(happinessDelta);
        
        // Migration out
        if (pop.getHappiness() < 20 && tickCount % 2 == 0) {
            entity.die();
            map.addAlert("Residents leaving: Low Happiness");
        }
        
        if (pop.getAge() > 100) {
            entity.die();
        }
    }
}
