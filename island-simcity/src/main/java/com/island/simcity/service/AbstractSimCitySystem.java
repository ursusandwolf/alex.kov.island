package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.Entity;
import com.island.engine.ecs.EntityQuery;
import com.island.engine.ecs.EntitySystem;
import com.island.simcity.model.CityTile;
import java.util.List;

/**
 * Base class for SimCity systems following the ECS System pattern.
 */
public abstract class AbstractSimCitySystem implements EntitySystem<Entity> {
    protected final EntityQuery<Entity> entityQuery;

    protected AbstractSimCitySystem(List<Class<? extends Component>> requiredComponents) {
        this.entityQuery = new EntityQuery<>(requiredComponents);
    }

    protected void process(Entity entity, CityTile tile, int tickCount) {
        // To be implemented by subclasses
    }
}
