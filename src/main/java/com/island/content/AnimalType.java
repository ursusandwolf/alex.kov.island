package com.island.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * Flyweight: common data for a species.
 */
@Getter
@Builder
@AllArgsConstructor
public final class AnimalType {
    private final SpeciesKey speciesKey;
    private final String typeName;
    private final double weight;
    private final double foodForSaturation;
    private final double maxEnergy;
    private final int maxPerCell;
    private final int speed;
    private final int maxLifespan;
    private final Map<SpeciesKey, Integer> huntProbabilities;
    private final boolean isPredator;
    private final SizeClass sizeClass;

    // Data-driven behavioral flags
    private final boolean isColdBlooded;
    private final boolean isPackHunter;
    private final boolean isBiomass;
    private final boolean isPlant;
    private final double reproductionChance;

    // Data-driven settlement properties
    private final double presenceProb;
    private final double settlementBase;
    private final double settlementRange;

    public boolean canEat(SpeciesKey key) {
        return huntProbabilities.containsKey(key);
    }

    public int getHuntProbability(SpeciesKey key) {
        return huntProbabilities.getOrDefault(key, 0);
    }

    public Set<SpeciesKey> getPreySpecies() {
        return huntProbabilities.keySet();
    }

    public enum Action { MOVE, FEED, REPRODUCE }

    public int getTickInterval(Action action) {
        if (!isColdBlooded) {
            return 1;
        }
        return switch (action) {
            case MOVE -> com.island.config.SimulationConstants.COLD_BLOODED_MOVE_INTERVAL;
            case FEED -> com.island.config.SimulationConstants.COLD_BLOODED_FEED_INTERVAL;
            case REPRODUCE -> com.island.config.SimulationConstants.COLD_BLOODED_REPRO_INTERVAL;
        };
    }
}
