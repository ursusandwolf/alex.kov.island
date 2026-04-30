package com.island.content;

import static com.island.config.SimulationConstants.BASE_METABOLISM_BP;
import static com.island.config.SimulationConstants.SCALE_10K;
import static com.island.config.SimulationConstants.SCALE_1M;

import com.island.config.EnergyPolicy;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.NonFinal;

/**
 * Base class for all organisms using integer-based arithmetic.
 * Energy and weight are stored as long (SCALE_1M).
 * Rates and modifiers are handled via basis points (SCALE_10K).
 */
@Getter
public abstract class Organism implements com.island.util.Poolable, com.island.engine.Mortal {

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

    protected Organism(long maxEnergy, int maxLifespan) {
        this(maxEnergy, maxLifespan, EnergyPolicy.BIRTH_INITIAL.getPercent()); 
    }

    protected Organism(long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        this.maxEnergy = maxEnergy;
        this.maxLifespan = maxLifespan;
        this.isAlive = true;
        this.currentEnergy = (maxEnergy * initialEnergyPercent) / 100;
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

    public void init(long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        this.maxEnergy = maxEnergy;
        this.maxLifespan = maxLifespan;
        this.currentEnergy = (maxEnergy * initialEnergyPercent) / 100;
        this.isAlive = true;
        this.age = 0;
    }

    public void die() {
        this.isAlive = false;
    }

    public abstract String getTypeName();

    public int getEnergyPercentage() {
        return (maxEnergy == 0) ? 0 : (int) ((currentEnergy * 100) / maxEnergy);
    }

    public boolean canPerformAction() { 
        return getEnergyPercentage() >= EnergyPolicy.ACTION_MIN.getPercent(); 
    }

    private final java.util.concurrent.locks.ReentrantLock energyLock = new java.util.concurrent.locks.ReentrantLock();

    public boolean tryConsumeEnergy(long amount) {
        energyLock.lock();
        try {
            currentEnergy = Math.max(0, currentEnergy - amount);
            if (currentEnergy == 0 && isAlive) {
                isAlive = false;
            }
            return isAlive;
        } finally {
            energyLock.unlock();
        }
    }

    public void consumeEnergy(long amount) {
        tryConsumeEnergy(amount);
    }

    public void setEnergy(long energy) {
        energyLock.lock();
        try {
            this.currentEnergy = Math.min(energy, maxEnergy);
            if (this.currentEnergy == 0 && isAlive) {
                isAlive = false;
            }
        } finally {
            energyLock.unlock();
        }
    }

    public void addEnergy(long amount) {
        energyLock.lock();
        try {
            currentEnergy = Math.min(maxEnergy, currentEnergy + amount);
        } finally {
            energyLock.unlock();
        }
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
        return getEnergyPercentage() < com.island.config.SimulationConstants.STARVATION_THRESHOLD_PERCENT;
    }

    public long getWeight() {
        return SCALE_1M; // Default 1.0 unit
    }

    public long getDynamicMetabolismRate() {
        SizeClass sizeClass = SizeClass.fromWeight((double) getWeight() / SCALE_1M);
        long baseMetabolism = (maxEnergy * BASE_METABOLISM_BP) / SCALE_10K;
        return (baseMetabolism * sizeClass.getMetabolismModifierBP() / SCALE_10K)
                * getSpecialMetabolismModifierBP() / SCALE_10K;
    }

    protected int getSpecialMetabolismModifierBP() {
        return SCALE_10K; // 1.0
    }

    public boolean isHibernating() {
        return false;
    }

    public abstract SpeciesKey getSpeciesKey();
}
