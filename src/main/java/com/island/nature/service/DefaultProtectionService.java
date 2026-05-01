package com.island.nature.service;

import com.island.nature.config.Configuration;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultProtectionService implements ProtectionService {
    private final Configuration config;
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
            long threshold = (globalCapacity * config.getEndangeredPopulationThresholdBP()) / config.getScale10K(); 
            
            if (currentCount > 0 && currentCount < threshold) {
                int ratio1000 = (int) ((currentCount * 1000) / threshold);
                int diff = config.getEndangeredMaxHideChancePercent() - config.getEndangeredMinHideChancePercent();
                int hideChance = config.getEndangeredMaxHideChancePercent() - (ratio1000 * diff) / 1000;
                protectionMap.put(key, hideChance);
            }
        }

        this.cachedProtectionMap = Collections.unmodifiableMap(protectionMap);
        this.lastUpdateTick = currentTick;
    }
}
