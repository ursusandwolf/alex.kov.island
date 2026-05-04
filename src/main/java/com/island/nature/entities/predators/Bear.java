package com.island.nature.entities.predators;

import com.island.nature.entities.AnimalType;
import com.island.nature.entities.herbivores.Herbivore;

/**
 * Bear implementation with integer-based metabolism and hibernation logic.
 */
public class Bear extends AbstractPredator implements Herbivore {
    private static final int FULL_CYCLE = 150;
    private static final int SLEEP_PERIOD = 50;

    public Bear(AnimalType type) {
        super(type);
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        // Combines predator and herbivore modifiers
        return (getPredatorMetabolismModifierBP() * getHerbivoreMetabolismModifierBP()) / config.getScale10K();
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
        return getHerbivoreOffspringBonus();
    }

    @Override
    public boolean isHibernating() {
        int cycleTime = getAge() % FULL_CYCLE;
        return cycleTime < SLEEP_PERIOD;
    }

    @Override
    public boolean canPerformAction() {
        return super.canPerformAction() && !isHibernating();
    }
}
