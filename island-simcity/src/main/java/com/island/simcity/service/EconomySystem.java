package com.island.simcity.service;

import com.island.engine.ecs.Entity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.BuildingProfile;
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
    public void beforeTick(int tickCount) {
        tickIncome.set(0);
        tickExpenses.set(0);
    }

    @Override
    protected void process(SimEntity entity, CityTile tile, int tickCount) {
        BuildingComponent building = entity.getComponent(BuildingComponent.class);
        PopulationComponent pop = entity.getComponent(PopulationComponent.class);

        if (building != null) {
            BuildingProfile profile = BuildingProfile.of(building.getType());
            long densityMultiplier = BuildingProfile.getDensityMultiplier(building.getDensity());

            tickExpenses.addAndGet(profile.getBaseExpense() * densityMultiplier);

            if (tile.isConnected()) {
                long incomePowerPenalty = tile.isPowered() ? 1 : 2;
                long income = (profile.getBaseIncome() * densityMultiplier) / incomePowerPenalty;
                tickIncome.addAndGet(income);
            }
        } else if (pop != null && tile.isConnected()) {
            tickIncome.addAndGet(map.getTaxRate());
        }
    }

    @Override
    public void afterTick(int tickCount) {
        map.setLastTickIncome(tickIncome.get());
        map.setLastTickExpenses(tickExpenses.get());
        map.addMoney(tickIncome.get() - tickExpenses.get());
    }
}
