package com.island.content.animals.predators;


import com.island.content.animals.herbivores.Herbivore;
import com.island.content.AnimalType;

/**
 * Bear implementation with a unique Hibernation mechanic.
 * Cycle: 100 ticks active, 50 ticks asleep (no hunting, no moving).
 * Bear is an Omnivore (Predator + Herbivore).
 */
public class Bear extends AbstractPredator implements Herbivore {
    private static final int FULL_CYCLE = 150;
    private static final int SLEEP_PERIOD = 50;

    public Bear(AnimalType type) {
        super(type);
    }

    @Override
    protected double getSpecialMetabolismModifier() {
        // Combines predator and herbivore modifiers
        return getPredatorMetabolismModifier() * getHerbivoreMetabolismModifier();
    }

    @Override
    public int getOffspringBonus() {
        return getHerbivoreOffspringBonus();
    }

    @Override
    public boolean isHibernating() {
        // Start with SLEEP_PERIOD, then ACTIVE_PERIOD
        int cycleTime = getAge() % FULL_CYCLE;
        return cycleTime < SLEEP_PERIOD;
    }

    @Override
    public boolean canPerformAction() {
        return super.canPerformAction() && !isHibernating();
    }
}
