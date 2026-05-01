package com.island.nature.entities;

import static com.island.nature.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER_BP;
import static com.island.nature.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;
import static com.island.nature.config.SimulationConstants.REPTILE_METABOLISM_MODIFIER_BP;
import static com.island.nature.config.SimulationConstants.SCALE_10K;

import com.island.nature.entities.herbivores.Herbivore;
import com.island.nature.entities.predators.Predator;

/**
 * A generic animal implementation that uses AnimalType for all its properties.
 * Uses integer-based arithmetic for metabolism.
 */
public class GenericAnimal extends Animal implements Herbivore, Predator {
    private final boolean isHerbivore;
    private final boolean isColdBlooded;

    public GenericAnimal(AnimalType type) {
        super(type);
        // If an animal can eat plants, we treat it as a herbivore for metabolic bonuses
        this.isHerbivore = type.canEat(SpeciesKey.PLANT) 
                        || type.canEat(SpeciesKey.GRASS) 
                        || type.canEat(SpeciesKey.CABBAGE);
        this.isColdBlooded = type.isColdBlooded();
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        int modifier = isHerbivore ? HERBIVORE_METABOLISM_MODIFIER_BP : SCALE_10K;
        if (isColdBlooded) {
            modifier = (modifier * REPTILE_METABOLISM_MODIFIER_BP) / SCALE_10K;
        }
        return modifier;
    }

    @Override
    public int getOffspringBonus() {
        return isHerbivore ? HERBIVORE_OFFSPRING_BONUS : 0;
    }
}
