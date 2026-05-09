package com.island.simcity.service;

import com.island.engine.ecs.Entity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.island.simcity.entities.SimEntity;

/**
 * ECS System for managing city economy.
 */
public class EconomySystem extends AbstractSimCitySystem {
    private final CityMap map;
    private final AtomicLong tickIncome = new AtomicLong();
    private final AtomicLong tickExpenses = new AtomicLong();

    public EconomySystem(CityMap map) {
        super(List.of(BuildingComponent.class, PopulationComponent.class));
        this.map = map;
        this.entityQuery.bind(map.getComponentRegistry());
    }

    @Override
    protected void process(SimEntity entity, CityTile tile, int tickCount) {
        // TODO(MVP): migrate logic from EconomyService to this ECS System
    }
}
