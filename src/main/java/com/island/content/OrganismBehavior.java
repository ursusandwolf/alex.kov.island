package com.island.content;

import static com.island.config.SimulationConstants.*;

// Базовое поведение организмов
public interface OrganismBehavior {
    double eat();
    boolean move();
    Organism reproduce();
    void checkState();
    double getEnergyPercentage();

    default boolean canPerformAction() { 
        return getEnergyPercentage() >= ACTION_MIN_ENERGY_PERCENT; 
    }
    default boolean canOnlyEat() { 
        double e = getEnergyPercentage(); 
        return e > 0 && e < ACTION_MIN_ENERGY_PERCENT; 
    }
}
