package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.nature.config.EnergyPolicy;
import com.island.engine.Mortal;
import com.island.util.Poolable;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.experimental.NonFinal;

@Getter
public abstract class Organism implements Poolable, Mortal {
    protected final Configuration config;
    @NonFinal private volatile long currentEnergy; 
    @NonFinal private long maxEnergy; 
    @NonFinal private int age; 
    @NonFinal private int maxLifespan; 
    @NonFinal private volatile boolean isAlive;
    private final ReentrantLock energyLock = new ReentrantLock();

    protected Organism(Configuration config, long maxEnergy, int maxLifespan) {
        this(config, maxEnergy, maxLifespan, EnergyPolicy.BIRTH_INITIAL.getPercent()); 
    }

    protected Organism(Configuration config, long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        this.config = config;
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
        return getEnergyPercentage() < config.getStarvationThresholdPercent();
    }

    public long getWeight() {
        return config.getScale1M(); 
    }

    public long getDynamicMetabolismRate() {
        SizeClass sizeClass = SizeClass.fromWeight((double) getWeight() / config.getScale1M());
        long baseMetabolism = (maxEnergy * config.getBaseMetabolismBP()) / config.getScale10K();
        return (baseMetabolism * sizeClass.getMetabolismModifierBP() / config.getScale10K())
                * getSpecialMetabolismModifierBP() / config.getScale10K();
    }

    protected int getSpecialMetabolismModifierBP() {
        return config.getScale10K();
    }

    public boolean isHibernating() {
        return false;
    }

    public abstract SpeciesKey getSpeciesKey();
}
