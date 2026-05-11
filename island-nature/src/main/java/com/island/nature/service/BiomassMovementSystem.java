package com.island.nature.service;

import com.island.engine.ecs.Component;
import com.island.nature.entities.components.GrowthComponent;
import com.island.nature.entities.components.MovementComponent;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.registry.BiomassManager;
import com.island.nature.model.Cell;
import com.island.util.common.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * ECS System responsible for mobile biomass movement.
 */
public class BiomassMovementSystem extends NatureEntitySystem {
    private final BiomassManager biomassManager;

    public BiomassMovementSystem(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.biomassManager = world;
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(MovementComponent.class);
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        return List.of(GrowthComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_MOVEMENT;
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        Biomass b = (Biomass) entity;
        if (b.getSpeed() <= 0 || b.getBiomass() <= 0) {
            return;
        }

        long totalMass = b.getBiomass();
        long chunk = (totalMass * config.getBiomassMoveChunkBP()) / config.getScale10K();

        int direction = getRandom().nextInt(4);
        int dx = 0;
        int dy = 0;
        switch (direction) {
            case 0 -> dy = -1;
            case 1 -> dy = 1;
            case 2 -> dx = -1;
            case 3 -> dx = 1;
            default -> { }
        }

        getWorld().getCell(cell, dx, dy).ifPresent(targetCell -> {
            if (targetCell != cell) {
                biomassManager.moveBiomassPartially(b, cell, targetCell, chunk);
            }
        });
    }
}
