package com.island.simcity.service;

import com.island.engine.ecs.Entity;
import com.island.engine.ecs.EntitySystem;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ECS System for managing city economy.
 */
public class EconomySystem extends EntitySystem<Entity> {
    private final CityMap map;
    private final AtomicLong tickIncome = new AtomicLong();
    private final AtomicLong tickExpenses = new AtomicLong();

    public EconomySystem(CityMap map) {
        super(List.of(BuildingComponent.class, PopulationComponent.class), List.of());
        this.map = map;
    }

    @Override
    public void process(Entity entity, int tickCount) {
        // Logic will iterate over relevant entities and update shared state
    }
}
