package com.island.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Flyweight: common data for a species.
 */
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

    // Data-driven settlement properties
    private final double presenceProb;
    private final double settlementBase;
    private final double settlementRange;

    public AnimalType(SpeciesKey speciesKey, String typeName, double weight, int maxPerCell,
                      int speed, double foodForSaturation, int maxLifespan,
                      Map<SpeciesKey, Integer> huntProbabilities,
                      boolean isColdBlooded, boolean isPackHunter, boolean isBiomass, boolean isPlant,
                      double presenceProb, double settlementBase, double settlementRange) {
        this.speciesKey = speciesKey;
        this.typeName = typeName;
        this.weight = weight;
        this.maxPerCell = maxPerCell;
        this.speed = speed;
        this.foodForSaturation = foodForSaturation;
        this.maxLifespan = maxLifespan;
        this.maxEnergy = foodForSaturation;
        this.huntProbabilities = (huntProbabilities != null) 
                ? Collections.unmodifiableMap(new HashMap<>(huntProbabilities)) 
                : Collections.emptyMap();
        this.isPredator = speciesKey.isPredator();
        this.sizeClass = SizeClass.fromWeight(weight);
        
        this.isColdBlooded = isColdBlooded;
        this.isPackHunter = isPackHunter;
        this.isBiomass = isBiomass;
        this.isPlant = isPlant;
        this.presenceProb = presenceProb;
        this.settlementBase = settlementBase;
        this.settlementRange = settlementRange;
    }

    public SpeciesKey getSpeciesKey() {
        return speciesKey;
    }

    public String getTypeName() {
        return typeName;
    }

    public double getWeight() {
        return weight;
    }

    public double getFoodForSaturation() {
        return foodForSaturation;
    }

    public double getMaxEnergy() {
        return maxEnergy;
    }

    public int getMaxPerCell() {
        return maxPerCell;
    }

    public int getSpeed() {
        return speed;
    }

    public int getMaxLifespan() {
        return maxLifespan;
    }

    public boolean isPredator() {
        return isPredator;
    }

    public SizeClass getSizeClass() {
        return sizeClass;
    }

    public boolean isColdBlooded() {
        return isColdBlooded;
    }

    public boolean isPackHunter() {
        return isPackHunter;
    }

    public boolean isBiomass() {
        return isBiomass;
    }

    public boolean isPlant() {
        return isPlant;
    }

    public double getPresenceProb() {
        return presenceProb;
    }

    public double getSettlementBase() {
        return settlementBase;
    }

    public double getSettlementRange() {
        return settlementRange;
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
}
