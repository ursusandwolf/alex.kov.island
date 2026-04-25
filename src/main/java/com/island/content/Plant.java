package com.island.content;

import lombok.Getter;

// Базовый класс растений
@Getter
public abstract class Plant extends Organism {
    protected double biomass; // Биомасса в кг (служит энергией)
    protected final double maxBiomass;
    protected final double growthRate; // Рост за тик в кг

    protected Plant(double maxBiomass, double growthRate, int maxLifespan) {
        super(maxBiomass, maxLifespan); // Use maxBiomass as energy capacity
        this.maxBiomass = maxBiomass;
        this.biomass = maxBiomass * 0.5; // Start with 50%
        this.growthRate = growthRate;
    }

    public double getBiomass() {
        return biomass;
    }

    public double consumeBiomass(double amount) {
        double actual = Math.min(biomass, amount);
        biomass -= actual;
        if (biomass <= 0) die();
        return actual;
    }

    public void grow() {
        if (isAlive()) biomass = Math.min(maxBiomass, biomass + growthRate);
    }

    @Override
    public double eat() { return 0; } // Растения не едят

    @Override
    public boolean move() { return false; } // Растения статичны

    @Override
    public Plant reproduce() {
        if (!canPerformAction()) return null;
        return null; // TODO: Реализовать механизм распространения
    }
}
