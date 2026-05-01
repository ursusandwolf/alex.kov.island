package com.island.service;

import static com.island.config.SimulationConstants.ENDANGERED_MAX_HIDE_CHANCE_PERCENT;
import static com.island.config.SimulationConstants.ENDANGERED_MIN_HIDE_CHANCE_PERCENT;
import static com.island.config.SimulationConstants.ENDANGERED_POPULATION_THRESHOLD_BP;
import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.content.AnimalType;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultProtectionService implements ProtectionService {
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    private final int worldArea;

    private Map<SpeciesKey, Integer> cachedProtectionMap = Collections.emptyMap();
    private int lastUpdateTick = -1;

    @Override
    public Map<SpeciesKey, Integer> getProtectionModifiers() {
        return cachedProtectionMap;
    }

    @Override
    public void update(int currentTick) {
        if (currentTick == lastUpdateTick) {
            return;
        }

        Map<SpeciesKey, Integer> protectionMap = new HashMap<>();

        for (SpeciesKey key : registry.getAllAnimalKeys()) {
            AnimalType type = registry.getAnimalType(key).orElse(null);
            if (type == null) {
                continue;
            }

            int currentCount = statisticsService.getSpeciesCount(key);
            long globalCapacity = (long) worldArea * type.getMaxPerCell();
            long threshold = (globalCapacity * ENDANGERED_POPULATION_THRESHOLD_BP) / SCALE_10K; 
            
            if (currentCount > 0 && currentCount < threshold) {
                int ratio1000 = (int) ((currentCount * 1000) / threshold);
                int diff = ENDANGERED_MAX_HIDE_CHANCE_PERCENT - ENDANGERED_MIN_HIDE_CHANCE_PERCENT;
                int hideChance = ENDANGERED_MAX_HIDE_CHANCE_PERCENT - (ratio1000 * diff) / 1000;
                protectionMap.put(key, hideChance);
            }
        }

        this.cachedProtectionMap = Collections.unmodifiableMap(protectionMap);
        this.lastUpdateTick = currentTick;
    }
}
