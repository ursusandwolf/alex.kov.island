package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

/**
 * Legacy economy service. Logic migrated to {@link EconomySystem}.
 * Keeping as a placeholder for potential non-ECS global economic tasks.
 */
@RequiredArgsConstructor
public class EconomyService extends AbstractSimCityService {
    private final CityMap map;

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of();
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        // Migrated to EconomySystem (ECS)
    }
}
