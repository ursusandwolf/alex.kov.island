package com.island.content;

import java.util.*;

/**
 * Flyweight: common data for a species.
 */
public final class AnimalType {
    private final String speciesKey, typeName;
    private final double weight, foodForSaturation, maxEnergy;
    private final int maxPerCell, speed, maxLifespan;
    private final Map<String, Integer> huntProbabilities;
    private final boolean isPredator;

    public AnimalType(String speciesKey, String typeName, double weight, int maxPerCell,
                      int speed, double foodForSaturation, int maxLifespan,
                      Map<String, Integer> huntProbabilities) {
        this.speciesKey = speciesKey;
        this.typeName = typeName;
        this.weight = weight;
        this.maxPerCell = maxPerCell;
        this.speed = speed;
        this.foodForSaturation = foodForSaturation;
        this.maxLifespan = maxLifespan;
        this.maxEnergy = foodForSaturation;
        this.huntProbabilities = (huntProbabilities != null) ? 
                Collections.unmodifiableMap(new HashMap<>(huntProbabilities)) : Collections.emptyMap();
        this.isPredator = this.huntProbabilities.keySet().stream()
                .anyMatch(key -> !key.equalsIgnoreCase("plant"));
    }

    public String getSpeciesKey() { return speciesKey; }
    public String getTypeName() { return typeName; }
    public double getWeight() { return weight; }
    public double getFoodForSaturation() { return foodForSaturation; }
    public double getMaxEnergy() { return maxEnergy; }
    public int getMaxPerCell() { return maxPerCell; }
    public int getSpeed() { return speed; }
    public int getMaxLifespan() { return maxLifespan; }
    public boolean isPredator() { return isPredator; }

    public boolean canEat(String key) { return huntProbabilities.containsKey(key); }
    public int getHuntProbability(String key) { return huntProbabilities.getOrDefault(key, 0); }
    public Set<String> getPreySpecies() { return huntProbabilities.keySet(); }
}
