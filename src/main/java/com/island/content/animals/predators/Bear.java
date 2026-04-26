package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

/**
 * Bear implementation with a unique Hibernation mechanic.
 * Cycle: 100 ticks active, 50 ticks asleep (no energy loss).
 */
public class Bear extends Animal implements Predator {
    private static final int ACTIVE_PERIOD = 100;
    private static final int SLEEP_PERIOD = 50;
    private static final int FULL_CYCLE = ACTIVE_PERIOD + SLEEP_PERIOD;

    public Bear(AnimalType type) {
        super(type);
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

    @Override
    public Bear reproduce() {
        if (isHibernating()) return null;
        return trySpendEnergyForReproduction() ? new Bear(animalType) : null;
    }
}
