package com.island.content;

import lombok.Getter;

/**
 * Categorizes organisms by weight to unify metabolism and hunt logic.
 */
@Getter
public enum SizeClass {
    TINY(1.25, 5, Lod.AGGREGATED),    // <= 0.1 kg (Caterpillar, Mouse)
    SMALL(1.20, 2, Lod.AGGREGATED),   // <= 1.0 kg (Duck)
    NORMAL(1.00, 1, Lod.INDIVIDUAL),  // <= 10.0 kg (Rabbit, Fox, Eagle)
    MEDIUM(1.00, 1, Lod.INDIVIDUAL),  // <= 150.0 kg (Boa, Goat, Sheep, Wolf)
    LARGE(0.80, 1, Lod.INDIVIDUAL),   // <= 500.0 kg (Horse, Deer, Boar)
    HUGE(0.80, 1, Lod.INDIVIDUAL);    // > 500.0 kg (Bear, Buffalo)

    private final double metabolismModifier;
    private final int offspringCount;
    private final Lod preferredLod;

    SizeClass(double metabolismModifier, int offspringCount, Lod preferredLod) {
        this.metabolismModifier = metabolismModifier;
        this.offspringCount = offspringCount;
        this.preferredLod = preferredLod;
    }

    public enum Lod {
        BIOMASS,      // Pure mass (Plants)
        AGGREGATED,   // Swarm/Group (Insects, small rodents)
        INDIVIDUAL    // Individual tracking (Predators, large herbivores)
    }

    public static SizeClass fromWeight(double weight) {
        if (weight <= 0.1) {
            return TINY;
        }
        if (weight <= 1.0) {
            return SMALL;
        }
        if (weight <= 10.0) {
            return NORMAL;
        }
        if (weight <= 150.0) {
            return MEDIUM;
        }
        if (weight <= 500.0) {
            return LARGE;
        }
        return HUGE;
    }
}
