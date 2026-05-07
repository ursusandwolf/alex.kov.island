package com.island.nature.service;

import com.island.engine.ecs.EntityQuery;
import com.island.engine.ecs.EntitySystem;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.domain.NatureWorld;
import com.island.util.common.RandomProvider;
import com.island.nature.model.Cell;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Base class for Nature domain services that follow the ECS System pattern.
 */
public abstract class NatureEntitySystem extends AbstractService implements EntitySystem<Organism> {
    private final EntityQuery<Organism> entityQuery;

    protected NatureEntitySystem(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.entityQuery = new EntityQuery<>(
            Stream.concat(readComponents().stream(), writeComponents().stream())
                  .distinct()
                  .toList()
        );
    }

    @Override
    protected void doProcessCell(Cell cell, int tickCount) {
        cell.query(entityQuery, entity -> process(entity, cell, tickCount));
    }

    protected abstract void process(Organism entity, Cell cell, int tickCount);
}
