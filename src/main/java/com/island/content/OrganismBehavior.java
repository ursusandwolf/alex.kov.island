package com.island.content;

// Базовое поведение организмов
public interface OrganismBehavior extends Moveable, Eatable {
    Organism reproduce();
    void checkState();
    double getEnergyPercentage();

    default boolean canPerformAction() { return getEnergyPercentage() >= 30.0; }
    default boolean canOnlyEat() { 
        double e = getEnergyPercentage(); 
        return e > 0 && e < 30.0; 
    }
}
