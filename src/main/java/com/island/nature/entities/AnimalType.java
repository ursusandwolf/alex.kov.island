package com.island.nature.entities;

import static com.island.nature.config.SimulationConstants.COLD_BLOODED_FEED_INTERVAL;
import static com.island.nature.config.SimulationConstants.COLD_BLOODED_MOVE_INTERVAL;
import static com.island.nature.config.SimulationConstants.COLD_BLOODED_REPRO_INTERVAL;
import static com.island.nature.config.SimulationConstants.SCALE_1M;

import com.island.nature.model.TerrainType;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Flyweight: common data for a species.
 * Uses integer-based arithmetic: weights/energy are long (SCALE_1M), chances are int (0-100).
 */
@Getter
@Builder
@AllArgsConstructor
public final class AnimalType {
    private final SpeciesKey speciesKey;
    private final String typeName;
    private final long weight;
    private final long foodForSaturation;
    private final long maxEnergy;
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
    private final int reproductionChance; // 0-100
    private final int maxOffspring;

    // Data-driven settlement properties
    private final int presenceChance; // 0-100
    private final long settlementBase;
    private final long settlementRange;

    // Terrain accessibility
    @Builder.Default private final boolean canFly = false;
    @Builder.Default private final boolean canSwim = false;
    @Builder.Default private final boolean canWalk = true;

    public boolean isTerrainAccessible(TerrainType terrain) {
        if (canFly) {
            return true;
        }
        if (canSwim && terrain.isWaterAccessible()) {
            return true;
        }
        if (canWalk && terrain.isLandAccessible()) {
            return true;
        }
        return false;
    }

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
            case MOVE -> COLD_BLOODED_MOVE_INTERVAL;
            case FEED -> COLD_BLOODED_FEED_INTERVAL;
            case REPRODUCE -> COLD_BLOODED_REPRO_INTERVAL;
        };
    }
}
