package com.island.content;

import static com.island.config.SimulationConstants.BASE_METABOLISM_PERCENT;
import static com.island.config.SimulationConstants.DEATH_EPSILON;

import com.island.config.EnergyPolicy;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;

/**
 * Базовый класс организмов.
 */
@Getter
public abstract class Organism implements com.island.util.Poolable, com.island.engine.Mortal {
    private final String id = java.util.UUID.randomUUID().toString();
    @NonFinal @Setter
    private volatile double currentEnergy; 
    @NonFinal
    private double maxEnergy; 
    @NonFinal
    private int age; 
    @NonFinal
    private int maxLifespan; 
    @NonFinal
    private volatile boolean isAlive;

    protected Organism(double maxEnergy, int maxLifespan) {
        this(maxEnergy, maxLifespan, EnergyPolicy.BIRTH_INITIAL.getFactor()); 
    }

    protected Organism(double maxEnergy, int maxLifespan, double energyFactor) {
        this.maxEnergy = maxEnergy;
        this.maxLifespan = maxLifespan;
        this.isAlive = true;
        this.currentEnergy = maxEnergy * energyFactor;
        this.age = 0;
    }

    @Override
    public void reset() {
        this.isAlive = false;
        this.age = 0;
        this.currentEnergy = 0; 
        this.maxEnergy = 0;
        this.maxLifespan = 0;
    }

    public void init(double maxEnergy, int maxLifespan, double energyFactor) {
        this.maxEnergy = maxEnergy;
        this.maxLifespan = maxLifespan;
        this.currentEnergy = maxEnergy * energyFactor;
        this.isAlive = true;
        this.age = 0;
    }

    public void die() {
        this.isAlive = false;
    }

    public abstract String getTypeName();

    public double getEnergyPercentage() {
        return (maxEnergy == 0) ? 0 : (currentEnergy / maxEnergy) * 100.0;
    }

    public boolean canPerformAction() { 
        return getEnergyPercentage() >= EnergyPolicy.ACTION_MIN.getPercent(); 
    }

    public void setEnergyFactor(double factor) {
        this.currentEnergy = maxEnergy * Math.max(0.0, Math.min(1.0, factor));
    }

    private final java.util.concurrent.locks.ReentrantLock energyLock = new java.util.concurrent.locks.ReentrantLock();

    public boolean tryConsumeEnergy(double amount) {
        energyLock.lock();
        try {
            currentEnergy = Math.max(0, currentEnergy - amount);
            if (currentEnergy < DEATH_EPSILON && isAlive) {
                isAlive = false;
            }
            return isAlive;
        } finally {
            energyLock.unlock();
        }
    }

    public void consumeEnergy(double amount) {
        tryConsumeEnergy(amount);
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

    public boolean isStarving() {
        return currentEnergy < DEATH_EPSILON;
    }

    public double getWeight() {
        return 1.0;
    }

    public double getDynamicMetabolismRate() {
        SizeClass sizeClass = SizeClass.fromWeight(getWeight());
        return maxEnergy * BASE_METABOLISM_PERCENT * sizeClass.getMetabolismModifier()
                * getSpecialMetabolismModifier();
    }

    protected double getSpecialMetabolismModifier() {
        return 1.0;
    }

    public boolean isHibernating() {
        return false;
    }

    public abstract SpeciesKey getSpeciesKey();
}
