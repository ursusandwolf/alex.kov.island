package com.island.simcity.service;

import com.island.engine.core.SimulationNode;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.Entity;
import com.island.engine.ecs.EntityQuery;
import com.island.engine.ecs.EntitySystem;
import com.island.simcity.model.CityTile;
import java.util.List;

import com.island.simcity.entities.SimEntity;

/**
 * Base class for SimCity systems following the ECS System pattern.
 */
public abstract class AbstractSimCitySystem implements EntitySystem<SimEntity> {
    protected final EntityQuery<SimEntity> entityQuery;

    protected AbstractSimCitySystem(List<Class<? extends Component>> requiredComponents) {
        this.entityQuery = new EntityQuery<>(requiredComponents);
    }

    @Override
    public void processCell(SimulationNode<SimEntity> node, int tickCount) {
        if (node instanceof CityTile tile) {
            node.query(entityQuery, entity -> process(entity, tile, tickCount));
        }
    }

    protected abstract void process(SimEntity entity, CityTile tile, int tickCount);
}
