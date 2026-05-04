package com.island.nature.entities.predators;

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
        return (config.getHerbivoreMetabolismModifierBP() * config.getReptileMetabolismModifierBP()) / config.getScale10K();
    }

    @Override
    public int getHerbivoreMetabolismModifierBP() {
        return config.getHerbivoreMetabolismModifierBP();
    }

    @Override
    public int getHerbivoreOffspringBonus() {
        return config.getHerbivoreOffspringBonus();
    }

    @Override
    public int getOffspringBonus() {
        return config.getHerbivoreOffspringBonus();
    }
}
