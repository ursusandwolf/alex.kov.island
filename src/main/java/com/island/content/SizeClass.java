package com.island.content;

import lombok.Getter;

/**
 * Categorizes organisms by weight to unify metabolism and hunt logic.
 */
@Getter
public enum SizeClass {
    TINY(12500, 5, Lod.AGGREGATED),    // <= 0.1 kg (Caterpillar, Mouse). 1.25x metabolism.
    SMALL(12000, 2, Lod.AGGREGATED),   // <= 1.0 kg (Duck). 1.20x metabolism.
    NORMAL(10000, 1, Lod.INDIVIDUAL),  // <= 10.0 kg (Rabbit, Fox, Eagle)
    MEDIUM(10000, 1, Lod.INDIVIDUAL),  // <= 150.0 kg (Boa, Goat, Sheep, Wolf)
    LARGE(8000, 1, Lod.INDIVIDUAL),   // <= 500.0 kg (Horse, Deer, Boar). 0.80x metabolism.
    HUGE(8000, 1, Lod.INDIVIDUAL);    // > 500.0 kg (Bear, Buffalo)

    private final int metabolismModifierBP; // SCALE_10K
    private final int offspringCount;
    private final Lod preferredLod;

    SizeClass(int metabolismModifierBP, int offspringCount, Lod preferredLod) {
        this.metabolismModifierBP = metabolismModifierBP;
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
