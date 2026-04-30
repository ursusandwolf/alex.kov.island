package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.animals.herbivores.Herbivore;

import static com.island.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER_BP;
import static com.island.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;
import static com.island.config.SimulationConstants.REPTILE_METABOLISM_MODIFIER_BP;
import static com.island.config.SimulationConstants.SCALE_10K;

/**
 * Chameleon with integer-based arithmetic.
 * 95% invisibility and highly efficient metabolism.
 */
public class Chameleon extends Animal implements Herbivore {
    private final com.island.util.RandomProvider random;

    public Chameleon(AnimalType type, com.island.util.RandomProvider random) {
        super(type);
        this.random = random;
    }

    @Override
    public boolean isProtected(int currentTick) {
        // Unique ability: 95% chance to be invisible to predators
        return super.isProtected(currentTick) || random.nextInt(0, 100) < 95;
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        // Cumulative bonus: Herbivore * Reptile
        return (HERBIVORE_METABOLISM_MODIFIER_BP * REPTILE_METABOLISM_MODIFIER_BP) / SCALE_10K;
    }

    @Override
    public int getOffspringBonus() {
        return HERBIVORE_OFFSPRING_BONUS;
    }
}
