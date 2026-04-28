package com.island.content;

import static com.island.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER;
import static com.island.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;

import com.island.content.animals.herbivores.Herbivore;
import com.island.content.animals.predators.Predator;

/**
 * A generic animal implementation that uses AnimalType for all its properties.
 * This class replaces simple species-specific marker classes (OCP).
 */
public class GenericAnimal extends Animal implements Herbivore, Predator {
    private final boolean isHerbivore;

    public GenericAnimal(AnimalType type) {
        super(type);
        // If an animal can eat plants, we treat it as a herbivore for metabolic bonuses
        this.isHerbivore = type.canEat(SpeciesKey.PLANT) 
                        || type.canEat(SpeciesKey.GRASS) 
                        || type.canEat(SpeciesKey.CABBAGE);
    }

    @Override
    protected double getSpecialMetabolismModifier() {
        return isHerbivore ? HERBIVORE_METABOLISM_MODIFIER : 1.0;
    }

    @Override
    public int getOffspringBonus() {
        return isHerbivore ? HERBIVORE_OFFSPRING_BONUS : 0;
    }
}
