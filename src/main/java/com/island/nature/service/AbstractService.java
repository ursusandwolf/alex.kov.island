package com.island.nature.service;

import com.island.nature.config.Configuration;
import com.island.engine.CellService;
import com.island.engine.SimulationNode;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.NatureEnvironment;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.Season;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.model.Cell;
import com.island.util.RandomProvider;
import com.island.util.SamplingUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * Base class for all simulation services using integer-based arithmetic.
 */
@Getter
public abstract class AbstractService implements CellService<Organism, SimulationNode<Organism>> {
    protected final Configuration config;
    private final NatureWorld world;
    private final NatureEnvironment environment;
    private final ExecutorService executor;
    private final RandomProvider random;
    protected Map<SpeciesKey, Integer> protectionMap; // Chance in percent (0-100)

    protected AbstractService(NatureWorld world, ExecutorService executor, RandomProvider random) {
        this.world = world;
        this.environment = world;
        this.config = world.getConfiguration();
        this.executor = executor;
        this.random = random;
    }

    @Override
    public void beforeTick(int tickCount) {
        // Shared logic: update protection map once per tick per service
        Map<SpeciesKey, Integer> map = environment.getProtectionMap();
        this.protectionMap = (map != null) ? map : Collections.emptyMap();
    }

    @Override
    public void tick(int tickCount) {
        beforeTick(tickCount);
        // Fallback for direct service usage (e.g. in tests)
        for (Collection<? extends SimulationNode<Organism>> unit : world.getParallelWorkUnits()) {
            for (SimulationNode<Organism> node : unit) {
                processCell(node, tickCount);
            }
        }
    }

    @Override
    public abstract void processCell(SimulationNode<Organism> node, int tickCount);

    protected <T> void forEachSampled(List<T> list, int limit, Consumer<T> action) {
        SamplingUtils.forEachSampled(list, limit, random, action);
    }

    protected boolean shouldAct(Animal animal, AnimalType.Action action, int tickCount) {
        if (!animal.canPerformAction() || isHibernating(animal)) {
            return false;
        }
        return (tickCount % animal.getAnimalType().getTickInterval(action) == 0);
    }

    private boolean isHibernating(Animal animal) {
        return animal.getAnimalType().isColdBlooded() && environment.getCurrentSeason() == Season.WINTER;
    }

    protected boolean isProtected(Animal animal) {
        if (animal.isProtected(0)) {
            return true;
        }
        if (protectionMap != null) {
            Integer chance = protectionMap.get(animal.getSpeciesKey());
            return chance != null && random.nextInt(0, 100) < chance;
        }
        return false;
    }

    protected boolean isPlantProtected(SpeciesKey speciesKey) {
        if (protectionMap != null) {
            Integer chance = protectionMap.get(speciesKey);
            return chance != null && random.nextInt(0, 100) < chance;
        }
        return false;
    }
}
