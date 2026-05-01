package com.island.nature.entities.predators;

import static com.island.nature.config.SimulationConstants.SCALE_10K;

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
        return (getPredatorMetabolismModifierBP() * getHerbivoreMetabolismModifierBP()) / SCALE_10K;
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
