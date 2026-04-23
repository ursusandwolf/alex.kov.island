package com.island.content;

// Базовое поведение организмов
public interface OrganismBehavior {
    double eat();
    boolean move();
    Organism reproduce();
    void checkState();
    double getEnergyPercentage();

    default boolean canPerformAction() { return getEnergyPercentage() >= 30.0; }
    default boolean canOnlyEat() { 
        double e = getEnergyPercentage(); 
        return e > 0 && e < 30.0; 
    }
}
