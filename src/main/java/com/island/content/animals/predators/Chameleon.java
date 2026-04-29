package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.animals.herbivores.Herbivore;
import com.island.util.RandomUtils;

import static com.island.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER;
import static com.island.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;
import static com.island.config.SimulationConstants.REPTILE_METABOLISM_MODIFIER;

/**
 * Chameleon has a unique protection mechanic: 95% invisibility.
 * It also uses combined Herbivore and Reptile metabolism for better survival.
 */
public class Chameleon extends Animal implements Herbivore {
    public Chameleon(AnimalType type) {
        super(type);
    }

    @Override
    public boolean isProtected(int currentTick) {
        // Unique ability: 95% chance to be invisible to predators
        return super.isProtected(currentTick) || RandomUtils.nextDouble() < 0.95;
    }

    @Override
    protected double getSpecialMetabolismModifier() {
        // Cumulative bonus: Herbivore (0.8) * Reptile (0.4) = 0.32x cost
        return HERBIVORE_METABOLISM_MODIFIER * REPTILE_METABOLISM_MODIFIER;
    }

    @Override
    public int getOffspringBonus() {
        return HERBIVORE_OFFSPRING_BONUS;
    }
}
