package com.island.content;

import java.util.UUID;
import static com.island.config.SimulationConstants.*;

// Базовый класс организмов
public abstract class Organism {
    private final String id; 
    private volatile double currentEnergy; 
    private final double maxEnergy; 
    private int age; 
    private final int maxLifespan; 
    private volatile boolean isAlive;
    protected boolean isHiding = false;

    protected Organism(double maxEnergy, int maxLifespan) {
        this(maxEnergy, maxLifespan, BABY_INITIAL_ENERGY_PERCENT / 100.0); 
    }

    protected Organism(double maxEnergy, int maxLifespan, double energyFactor) {
        this.id = UUID.randomUUID().toString();
        this.maxEnergy = maxEnergy;
        this.currentEnergy = maxEnergy * energyFactor;
        this.age = 0;
        this.maxLifespan = maxLifespan;
        this.isAlive = true;
    }

    public String getId() { return id; }
    public boolean isAlive() { return isAlive; }
    public boolean isHiding() { return isHiding; }
    public void setHiding(boolean h) { this.isHiding = h; }
    protected void die() { this.isAlive = false; }
    public double getCurrentEnergy() { return currentEnergy; }
    public double getMaxEnergy() { return maxEnergy; }
    public int getAge() { return age; }

    public abstract String getTypeName();

    public double getEnergyPercentage() {
        return (maxEnergy == 0) ? 0 : (currentEnergy / maxEnergy) * 100.0;
    }

    public boolean canPerformAction() { 
        return getEnergyPercentage() >= ACTION_MIN_ENERGY_PERCENT; 
    }

    public boolean canOnlyEat() { 
        double e = getEnergyPercentage(); 
        return e > (DEATH_EPSILON * 100) && e < ACTION_MIN_ENERGY_PERCENT; 
    }

    public void setEnergyFactor(double factor) {
        this.currentEnergy = maxEnergy * Math.max(0.0, Math.min(1.0, factor));
    }

    public synchronized void consumeEnergy(double amount) {
        currentEnergy = Math.max(0, currentEnergy - amount);
        if (currentEnergy < DEATH_EPSILON && isAlive) {
            isAlive = false;
        }
    }

    public void setEnergy(double energy) {
        this.currentEnergy = Math.min(energy, maxEnergy);
        if (this.currentEnergy < DEATH_EPSILON && isAlive) {
            isAlive = false;
        }
    }

    public void addEnergy(double amount) {
        currentEnergy = Math.min(maxEnergy, currentEnergy + amount);
    }

    public boolean checkAgeDeath() {
        age++;
        if (maxLifespan > 0 && age >= maxLifespan && isAlive) {
            isAlive = false;
            return true;
        }
        return false;
    }

    protected void ageOneTick() {
        age++;
        if (maxLifespan > 0 && age >= maxLifespan) isAlive = false;
    }

    public boolean isStarving() {
        return currentEnergy < DEATH_EPSILON;
    }

    public double getWeight() { return 1.0; } // Default weight

    /**
     * Stepped Metabolism Rate: 
     * Small organisms burn energy slightly faster, large ones slightly slower.
     */
    public double getDynamicMetabolismRate() {
        double w = getWeight();
        if (w < 1.0) return BASE_METABOLISM_PERCENT * METABOLISM_MODIFIER_TINY; 
        if (w > 300.0) return BASE_METABOLISM_PERCENT * METABOLISM_MODIFIER_LARGE;
        return BASE_METABOLISM_PERCENT * METABOLISM_MODIFIER_MEDIUM;
    }

    public void checkState() {
        // Now returns void, logic moved to service for reporting
    }

    public abstract String getSpeciesKey();
}
