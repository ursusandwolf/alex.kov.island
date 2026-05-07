package com.island.simcity.service;

import com.island.engine.core.SimulationNode;
import com.island.engine.service.CellService;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityTile;

/**
 * Base class for SimCity services that provides automatic node narrowing.
 */
public abstract class AbstractSimCityService implements CellService<SimEntity> {
    
    @Override
    public final void processCell(SimulationNode<SimEntity> node, int tickCount) {
        if (node instanceof CityTile tile) {
            doProcessTile(tile, tickCount);
        }
    }

    protected abstract void doProcessTile(CityTile tile, int tickCount);
}
