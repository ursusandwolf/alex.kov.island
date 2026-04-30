package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.SpeciesKey;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.Tickable;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base class for all simulation services using integer-based arithmetic.
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractService<N extends SimulationNode> implements Tickable {
    private final SimulationWorld world;
    private final ExecutorService executor;
    private final RandomProvider random;
    protected Map<SpeciesKey, Integer> protectionMap; // Chance in percent (0-100)

    @Override
    @SuppressWarnings("unchecked")
    public void tick(int tickCount) {
        if (executor.isShutdown()) {
            return;
        }

        // Shared logic: update protection map once per tick per service
        Map<SpeciesKey, Integer> map = world.getProtectionMap(null);
        this.protectionMap = (map != null) ? map : java.util.Collections.emptyMap();

        List<Callable<Void>> tasks = new ArrayList<>();
        Collection<? extends Collection<? extends SimulationNode>> workUnits = world.getParallelWorkUnits();
        
        if (workUnits.size() <= 1) {
            for (Collection<? extends SimulationNode> workUnit : workUnits) {
                for (SimulationNode node : workUnit) {
                    processCell((N) node, tickCount);
                }
            }
            return;
        }

        for (Collection<? extends SimulationNode> workUnit : workUnits) {
            tasks.add(() -> {
                for (SimulationNode node : workUnit) {
                    processCell((N) node, tickCount);
                }
                return null;
            });
        }
        try {
            if (!executor.isShutdown()) {
                executor.invokeAll(tasks);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Silently ignore
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected <T> void forEachSampled(List<T> list, int limit, Consumer<T> action) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        int step = (size > limit) ? (size / limit + 1) : 1;
        int startOffset = (size > limit) ? random.nextInt(step) : 0;
        for (int i = startOffset; i < size; i += step) {
            action.accept(list.get(i));
        }
    }

    protected abstract void processCell(N node, int tickCount);

    protected boolean shouldAct(Animal animal, AnimalType.Action action, int tickCount) {
        if (!animal.canPerformAction()) {
            return false;
        }
        return (tickCount % animal.getAnimalType().getTickInterval(action) == 0);
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
