package com.island.nature.entities.core;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.EntityIdProvider;
import com.island.engine.core.HealthStorage;
import com.island.engine.core.MovementStorage;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.ecs.ComponentStore;
import com.island.engine.ecs.EntityArchetype;
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
    private final ComponentRegistry componentRegistry;
    private final ComponentStore componentStore;
    private volatile EntityArchetype archetype;

    private int entityId = -1;
    private HealthStorage healthStorage;
    private AgeStorage ageStorage;
    private MovementStorage movementStorage;
    
    // Fallback state when not bound to SoA (e.g. tests or pre-initialization)
    private volatile long fallbackCurrentEnergy;
    private volatile long fallbackMaxEnergy;
    private volatile boolean fallbackAlive;
    private volatile int fallbackAge;
    private volatile int fallbackMaxLifespan;
    private volatile int fallbackSpeed;
    
    @Setter private volatile DeathCause lastDeathCause;

    protected Organism(Configuration config, ComponentRegistry registry, long maxEnergy, int maxLifespan) {
        this(config, registry, maxEnergy, maxLifespan, EnergyPolicy.BIRTH_INITIAL.getPercent());
    }

    protected Organism(Configuration config, ComponentRegistry registry, long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        this.config = config;
        this.componentRegistry = registry;
        this.componentStore = ComponentStore.createArray(registry);
        
        this.fallbackMaxEnergy = maxEnergy;
        this.fallbackCurrentEnergy = (maxEnergy * initialEnergyPercent) / 100;
        this.fallbackAlive = true;
        this.fallbackAge = 0;
        this.fallbackMaxLifespan = maxLifespan;
        
        // Add markers to maintain ECS compatibility
        addComponent(new HealthComponent());
        addComponent(new AgeComponent());
    }

    public void bindStorage(int entityId, HealthStorage healthStorage, AgeStorage ageStorage, MovementStorage movementStorage) {
        this.entityId = entityId;
        this.healthStorage = healthStorage;
        this.ageStorage = ageStorage;
        this.movementStorage = movementStorage;
        syncToStorage();
    }

    private void syncToStorage() {
        if (entityId != -1) {
            if (healthStorage != null) {
                healthStorage.set(entityId, fallbackCurrentEnergy, fallbackMaxEnergy, fallbackAlive);
            }
            if (ageStorage != null) {
                ageStorage.set(entityId, fallbackAge, fallbackMaxLifespan);
            }
            if (movementStorage != null) {
                movementStorage.set(entityId, fallbackSpeed);
            }
        }
    }

    public <C extends Component> void addComponent(C component) {
        componentStore.add(component);
        updateArchetype();
    }

    private void updateArchetype() {
        this.archetype = componentRegistry.getArchetype(componentStore.getComponentBitSet());
    }

    @Override
    public EntityArchetype getArchetype() {
        return archetype;
    }

    public <C extends Component> C getComponent(Class<C> type) {
        return componentStore.get(type);
    }

    @Override
    public void reset() {
        this.fallbackAlive = false;
        this.fallbackCurrentEnergy = 0;
        this.fallbackMaxEnergy = 0;
        this.fallbackAge = 0;
        this.fallbackMaxLifespan = 0;
        this.fallbackSpeed = 0;
        
        this.archetype = null;
        this.entityId = -1;
        this.healthStorage = null;
        this.ageStorage = null;
        this.movementStorage = null;
    }

    public void init(long maxEnergy, int maxLifespan, int initialEnergyPercent) {
        init(maxEnergy, maxLifespan, initialEnergyPercent, 0);
    }

    public void init(long maxEnergy, int maxLifespan, int initialEnergyPercent, int speed) {
        this.fallbackMaxEnergy = maxEnergy;
        this.fallbackCurrentEnergy = (maxEnergy * initialEnergyPercent) / 100;
        this.fallbackAlive = true;
        this.fallbackAge = 0;
        this.fallbackMaxLifespan = maxLifespan;
        this.fallbackSpeed = speed;
        
        this.lastDeathCause = null;
        updateArchetype();
        syncToStorage();
    }

    public boolean isAlive() {
        if (entityId != -1 && healthStorage != null) {
            return healthStorage.isAlive(entityId);
        }
        return fallbackAlive;
    }

    public void die() {
        die(null);
    }

    public void die(DeathCause cause) {
        this.fallbackAlive = false;
        if (entityId != -1 && healthStorage != null) {
            healthStorage.setAlive(entityId, false);
        }
        this.lastDeathCause = cause;
    }

    public abstract String getTypeName();

    public int getEnergyPercentage() {
        long current = getCurrentEnergy();
        long max = getMaxEnergy();
        return (max > 0) ? (int) ((current * 100) / max) : 0;
    }

    public boolean canPerformAction() {
        return getEnergyPercentage() >= EnergyPolicy.ACTION_MIN.getPercent();
    }

    public boolean tryConsumeEnergy(long amount) {
        if (isAlive()) {
            long current = getCurrentEnergy();
            long next = Math.max(0, current - amount);
            setEnergy(next);
            if (next == 0) {
                die(DeathCause.HUNGER);
            }
        }
        return isAlive();
    }

    public void consumeEnergy(long amount) {
        tryConsumeEnergy(amount);
    }

    public void setEnergy(long energy) {
        long max = getMaxEnergy();
        long val = Math.min(energy, max);
        
        this.fallbackCurrentEnergy = val;
        if (entityId != -1 && healthStorage != null) {
            healthStorage.setCurrentEnergy(entityId, val);
        }
        
        if (val == 0 && isAlive()) {
            die(DeathCause.HUNGER);
        }
    }

    public void addEnergy(long amount) {
        setEnergy(getCurrentEnergy() + amount);
    }

    public boolean checkAgeDeath() {
        int nextAge = getAge() + 1;
        
        this.fallbackAge = nextAge;
        if (entityId != -1 && ageStorage != null) {
            ageStorage.setAge(entityId, nextAge);
        }
        
        if (getMaxLifespan() > 0 && nextAge >= getMaxLifespan() && isAlive()) {
            die(DeathCause.AGE);
            return true;
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
        long maxEnergy = getMaxEnergy();
        if (maxEnergy == 0) {
            return 0;
        }
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

    public long getCurrentEnergy() {
        if (entityId != -1 && healthStorage != null) {
            return healthStorage.getCurrentEnergy(entityId);
        }
        return fallbackCurrentEnergy;
    }

    public long getMaxEnergy() {
        if (entityId != -1 && healthStorage != null) {
            return healthStorage.getMaxEnergy(entityId);
        }
        return fallbackMaxEnergy;
    }

    public int getAge() {
        if (entityId != -1 && ageStorage != null) {
            return ageStorage.getAge(entityId);
        }
        return fallbackAge;
    }

    public int getMaxLifespan() {
        if (entityId != -1 && ageStorage != null) {
            return ageStorage.getMaxLifespan(entityId);
        }
        return fallbackMaxLifespan;
    }

    public int getSpeed() {
        if (entityId != -1 && movementStorage != null) {
            return movementStorage.getSpeed(entityId);
        }
        return fallbackSpeed;
    }

    public void setSpeed(int speed) {
        this.fallbackSpeed = speed;
        if (entityId != -1 && movementStorage != null) {
            movementStorage.setSpeed(entityId, speed);
        }
    }
}
