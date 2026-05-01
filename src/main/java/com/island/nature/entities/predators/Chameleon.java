package com.island.nature.entities.predators;

import static com.island.nature.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER_BP;
import static com.island.nature.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;
import static com.island.nature.config.SimulationConstants.REPTILE_METABOLISM_MODIFIER_BP;
import static com.island.nature.config.SimulationConstants.SCALE_10K;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.herbivores.Herbivore;
import com.island.util.RandomProvider;

/**
 * Chameleon with integer-based arithmetic.
 * 95% invisibility and highly efficient metabolism.
 */
public class Chameleon extends Animal implements Herbivore {
    private final RandomProvider random;

    public Chameleon(AnimalType type, RandomProvider random) {
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
