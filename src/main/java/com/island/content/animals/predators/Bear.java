package com.island.content.animals.predators;

import com.island.content.AnimalType;

/**
 * Bear implementation with a unique Hibernation mechanic.
 * Cycle: 100 ticks active, 50 ticks asleep (no hunting, no moving).
 */
public class Bear extends Predator {
    private static final int FULL_CYCLE = 150;
    private static final int SLEEP_PERIOD = 50;

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
}
