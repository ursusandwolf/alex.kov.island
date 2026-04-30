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
    private static final long ENERGY_SCALE = 1_000_000L;

    private final String id = java.util.UUID.randomUUID().toString();
    @NonFinal
    private volatile long currentEnergy; 
    @NonFinal
    private long maxEnergy; 
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
        this.maxEnergy = (long) (maxEnergy * ENERGY_SCALE);
        this.maxLifespan = maxLifespan;
        this.isAlive = true;
        this.currentEnergy = (long) (this.maxEnergy * energyFactor);
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
        this.maxEnergy = (long) (maxEnergy * ENERGY_SCALE);
        this.maxLifespan = maxLifespan;
        this.currentEnergy = (long) (this.maxEnergy * energyFactor);
        this.isAlive = true;
        this.age = 0;
    }

    public void die() {
        this.isAlive = false;
    }

    public abstract String getTypeName();

    public double getEnergyPercentage() {
        return (maxEnergy == 0) ? 0 : ((double) currentEnergy / maxEnergy) * 100.0;
    }

    public double getCurrentEnergy() {
        return (double) currentEnergy / ENERGY_SCALE;
    }

    public double getMaxEnergy() {
        return (double) maxEnergy / ENERGY_SCALE;
    }

    public boolean canPerformAction() { 
        return getEnergyPercentage() >= EnergyPolicy.ACTION_MIN.getPercent(); 
    }

    public void setEnergyFactor(double factor) {
        this.currentEnergy = (long) (maxEnergy * Math.max(0.0, Math.min(1.0, factor)));
    }

    private final java.util.concurrent.locks.ReentrantLock energyLock = new java.util.concurrent.locks.ReentrantLock();

    public boolean tryConsumeEnergy(double amount) {
        long longAmount = (long) (amount * ENERGY_SCALE);
        energyLock.lock();
        try {
            currentEnergy = Math.max(0, currentEnergy - longAmount);
            if (currentEnergy < 1 && isAlive) { // Using 1 instead of EPSILON for long
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
        this.currentEnergy = Math.min((long) (energy * ENERGY_SCALE), maxEnergy);
        if (this.currentEnergy < 1 && isAlive) {
            isAlive = false;
        }
    }

    public void addEnergy(double amount) {
        currentEnergy = Math.min(maxEnergy, currentEnergy + (long) (amount * ENERGY_SCALE));
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
