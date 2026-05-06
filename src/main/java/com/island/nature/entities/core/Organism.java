package com.island.nature.entities.core;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentStore;
import com.island.engine.ecs.DefaultComponentStore;
import com.island.nature.config.Configuration;
import com.island.nature.config.EnergyPolicy;
import com.island.nature.entities.components.AgeComponent;
import com.island.nature.entities.components.HealthComponent;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;
import com.island.engine.model.Mortal;
import com.island.engine.ecs.Entity;
import com.island.util.common.Poolable;

@Getter
public abstract class Organism implements Poolable, Entity {
    protected final Configuration config;
    private final ComponentStore componentStore = new DefaultComponentStore();
    
    // Hot components optimization: direct fields to avoid Map lookup overhead
    private HealthComponent healthComponent;
    private AgeComponent ageComponent;
    
    @Setter private volatile DeathCause lastDeathCause;
    private final ReentrantLock energyLock = new ReentrantLock();

    protected Organism(Configuration config, long maxEnergy, int maxLifespan) {
        this(config, maxEnergy, maxLifespan, EnergyPolicy.BIRTH_INITIAL.getPercent());
    }

    protected Organism(Configuration config, long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        this.config = config;
        long currentEnergy = (maxEnergy * initialEnergyPercent) / 100;
        addComponent(new HealthComponent(currentEnergy, maxEnergy, true));
        addComponent(new AgeComponent(0, maxLifespan));
    }

    public <C extends Component> void addComponent(C component) {
        if (component instanceof HealthComponent hc) {
            this.healthComponent = hc;
        } else if (component instanceof AgeComponent ac) {
            this.ageComponent = ac;
        }
        componentStore.add(component);
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> C getComponent(Class<C> type) {
        if (type == HealthComponent.class) {
            return (C) healthComponent;
        }
        if (type == AgeComponent.class) {
            return (C) ageComponent;
        }
        return componentStore.get(type);
    }

    @Override
    public void reset() {
        if (healthComponent != null) {
            healthComponent.setAlive(false);
            healthComponent.setCurrentEnergy(0);
            healthComponent.setMaxEnergy(0);
        }
        if (ageComponent != null) {
            ageComponent.setAge(0);
            ageComponent.setMaxLifespan(0);
        }
    }

    public void init(long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        if (healthComponent != null) {
            healthComponent.setMaxEnergy(maxEnergy);
            healthComponent.setCurrentEnergy((maxEnergy * initialEnergyPercent) / 100);
            healthComponent.setAlive(true);
        }
        if (ageComponent != null) {
            ageComponent.setAge(0);
            ageComponent.setMaxLifespan(maxLifespan);
        }
        this.lastDeathCause = null;
    }

    public boolean isAlive() {
        return healthComponent != null && healthComponent.isAlive();
    }

    public void die() {
        die(null);
    }

    public void die(DeathCause cause) {
        if (healthComponent != null) {
            healthComponent.setAlive(false);
        }
        this.lastDeathCause = cause;
    }

    public abstract String getTypeName();

    public int getEnergyPercentage() {
        return (healthComponent != null) ? healthComponent.getEnergyPercentage() : 0;
    }

    public boolean canPerformAction() {
        return getEnergyPercentage() >= EnergyPolicy.ACTION_MIN.getPercent();
    }

    public boolean tryConsumeEnergy(long amount) {
        energyLock.lock();
        try {
            if (healthComponent != null && healthComponent.isAlive()) {
                healthComponent.setCurrentEnergy(Math.max(0, healthComponent.getCurrentEnergy() - amount));
                if (healthComponent.getCurrentEnergy() == 0) {
                    die(DeathCause.HUNGER);
                }
            }
            return isAlive();
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
            if (healthComponent != null) {
                healthComponent.setCurrentEnergy(Math.min(energy, healthComponent.getMaxEnergy()));
                if (healthComponent.getCurrentEnergy() == 0 && healthComponent.isAlive()) {
                    healthComponent.setAlive(false);
                }
            }
        } finally {
            energyLock.unlock();
        }
    }

    public void addEnergy(long amount) {
        energyLock.lock();
        try {
            if (healthComponent != null) {
                healthComponent.setCurrentEnergy(Math.min(healthComponent.getMaxEnergy(), healthComponent.getCurrentEnergy() + amount));
            }
        } finally {
            energyLock.unlock();
        }
    }

    public boolean checkAgeDeath() {
        if (ageComponent != null) {
            ageComponent.setAge(ageComponent.getAge() + 1);
            if (ageComponent.getMaxLifespan() > 0 && ageComponent.getAge() >= ageComponent.getMaxLifespan() && isAlive()) {
                die(DeathCause.AGE);
                return true;
            }
        }
        return false;
    }

    public boolean isHungry() {
        return getEnergyPercentage() < config.getHungerThresholdPercent();
    }

    public long getWeight() {
        return config.getScale1M();
    }

    public long getDynamicMetabolismRate() {
        if (healthComponent == null) {
            return 0;
        }
        SizeClass sizeClass = SizeClass.fromWeight((double) getWeight() / config.getScale1M());
        long baseMetabolism = (healthComponent.getMaxEnergy() * config.getBaseMetabolismBP()) / config.getScale10K();
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

    public long getCurrentEnergy() {
        return (healthComponent != null) ? healthComponent.getCurrentEnergy() : 0;
    }

    public long getMaxEnergy() {
        return (healthComponent != null) ? healthComponent.getMaxEnergy() : 0;
    }

    public int getAge() {
        return (ageComponent != null) ? ageComponent.getAge() : 0;
    }

    public int getMaxLifespan() {
        return (ageComponent != null) ? ageComponent.getMaxLifespan() : 0;
    }
}