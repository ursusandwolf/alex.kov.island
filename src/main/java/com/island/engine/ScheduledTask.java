package com.island.engine;

public interface ScheduledTask extends Tickable {
    Phase phase();

    int priority();

    boolean isParallelizable();
}