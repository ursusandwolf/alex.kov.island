package com.island.nature.entities;

import com.island.engine.Mortal;
import com.island.engine.ecs.Component;
import com.island.nature.config.Configuration;
import com.island.nature.config.EnergyPolicy;
import com.island.nature.entities.components.AgeComponent;
import com.island.nature.entities.components.HealthComponent;
import com.island.util.Poolable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Organism implements Poolable, Mortal {
    protected final Configuration config;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();
    @Setter private DeathCause lastDeathCause;
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
        components.put(component.getClass(), component);
    }

    public <C extends Component> C getComponent(Class<C> type) {
        return type.cast(components.get(type));
    }

    @Override
    public void reset() {
        HealthComponent health = getComponent(HealthComponent.class);
        AgeComponent ageComp = getComponent(AgeComponent.class);
        if (health != null) {
            health.setAlive(false);
            health.setCurrentEnergy(0);
            health.setMaxEnergy(0);
        }
        if (ageComp != null) {
            ageComp.setAge(0);
            ageComp.setMaxLifespan(0);
        }
    }

    public void init(long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        HealthComponent health = getComponent(HealthComponent.class);
        AgeComponent ageComp = getComponent(AgeComponent.class);
        if (health != null) {
            health.setMaxEnergy(maxEnergy);
            health.setCurrentEnergy((maxEnergy * initialEnergyPercent) / 100);
            health.setAlive(true);
        }
        if (ageComp != null) {
            ageComp.setAge(0);
            ageComp.setMaxLifespan(maxLifespan);
        }
        this.lastDeathCause = null;
    }

    public boolean isAlive() {
        HealthComponent health = getComponent(HealthComponent.class);
        return health != null && health.isAlive();
    }

    public void die() {
        die(null);
    }

    public void die(DeathCause cause) {
        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null) {
            health.setAlive(false);
        }
        this.lastDeathCause = cause;
    }

    public abstract String getTypeName();

    public int getEnergyPercentage() {
        HealthComponent health = getComponent(HealthComponent.class);
        return (health != null) ? health.getEnergyPercentage() : 0;
    }

    public boolean canPerformAction() {
        return getEnergyPercentage() >= EnergyPolicy.ACTION_MIN.getPercent();
    }

    public boolean tryConsumeEnergy(long amount) {
        energyLock.lock();
        try {
            HealthComponent health = getComponent(HealthComponent.class);
            if (health != null && health.isAlive()) {
                health.setCurrentEnergy(Math.max(0, health.getCurrentEnergy() - amount));
                if (health.getCurrentEnergy() == 0) {
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
            HealthComponent health = getComponent(HealthComponent.class);
            if (health != null) {
                health.setCurrentEnergy(Math.min(energy, health.getMaxEnergy()));
                if (health.getCurrentEnergy() == 0 && health.isAlive()) {
                    health.setAlive(false);
                }
            }
        } finally {
            energyLock.unlock();
        }
    }

    public void addEnergy(long amount) {
        energyLock.lock();
        try {
            HealthComponent health = getComponent(HealthComponent.class);
            if (health != null) {
                health.setCurrentEnergy(Math.min(health.getMaxEnergy(), health.getCurrentEnergy() + amount));
            }
        } finally {
            energyLock.unlock();
        }
    }

    public boolean checkAgeDeath() {
        AgeComponent ageComp = getComponent(AgeComponent.class);
        if (ageComp != null) {
            ageComp.setAge(ageComp.getAge() + 1);
            if (ageComp.getMaxLifespan() > 0 && ageComp.getAge() >= ageComp.getMaxLifespan() && isAlive()) {
                die(DeathCause.AGE);
                return true;
            }
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
        HealthComponent health = getComponent(HealthComponent.class);
        if (health == null) {
            return 0;
        }
        SizeClass sizeClass = SizeClass.fromWeight((double) getWeight() / config.getScale1M());
        long baseMetabolism = (health.getMaxEnergy() * config.getBaseMetabolismBP()) / config.getScale10K();
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
        HealthComponent health = getComponent(HealthComponent.class);
        return (health != null) ? health.getCurrentEnergy() : 0;
    }

    public long getMaxEnergy() {
        HealthComponent health = getComponent(HealthComponent.class);
        return (health != null) ? health.getMaxEnergy() : 0;
    }

    public int getAge() {
        AgeComponent ageComp = getComponent(AgeComponent.class);
        return (ageComp != null) ? ageComp.getAge() : 0;
    }

    public int getMaxLifespan() {
        AgeComponent ageComp = getComponent(AgeComponent.class);
        return (ageComp != null) ? ageComp.getMaxLifespan() : 0;
    }
}
