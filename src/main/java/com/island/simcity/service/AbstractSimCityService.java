package com.island.simcity.service;

import com.island.engine.core.SimulationNode;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.EntitySystem;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityTile;
import java.util.List;

/**
 * Base class for SimCity services that provides automatic node narrowing
 * and implements EntitySystem for ECS integration.
 */
public abstract class AbstractSimCityService implements EntitySystem<SimEntity> {
    
    @Override
    public final void processCell(SimulationNode<SimEntity> node, int tickCount) {
        if (node instanceof CityTile tile) {
            doProcessTile(tile, tickCount);
        }
    }

    protected abstract void doProcessTile(CityTile tile, int tickCount);

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of();
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        return List.of();
    }
}
