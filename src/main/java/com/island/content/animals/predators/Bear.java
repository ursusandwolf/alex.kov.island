package com.island.content.animals.predators;

import static com.island.config.SimulationConstants.SCALE_10K;
import com.island.content.animals.herbivores.Herbivore;
import com.island.content.AnimalType;

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
