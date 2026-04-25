package com.island.content;

import java.util.UUID;
import static com.island.config.SimulationConstants.*;

// Базовый класс организмов (Template Method, Strategy, Information Expert)
public abstract class Organism implements OrganismBehavior {
    private final String id; // Уникальный ID экземпляра
    private volatile double currentEnergy; // 0..maxEnergy
    private final double maxEnergy; // Макс. емкость энергии
    private int age; // Возраст в тиках
    private final int maxLifespan; // Макс. возраст (0=бессмертен)
    private volatile boolean isAlive;

    protected Organism(double maxEnergy, int maxLifespan) {
        this.id = UUID.randomUUID().toString();
        this.maxEnergy = maxEnergy;
        this.currentEnergy = maxEnergy;
        this.age = 0;
        this.maxLifespan = maxLifespan;
        this.isAlive = true;
    }

    public String getId() { return id; }
    public boolean isAlive() { return isAlive; }
    protected void die() { this.isAlive = false; }
    public double getCurrentEnergy() { return currentEnergy; }
    public double getMaxEnergy() { return maxEnergy; }
    public int getAge() { return age; }

    public abstract String getTypeName();

    @Override
    public double getEnergyPercentage() {
        return (maxEnergy == 0) ? 0 : (currentEnergy / maxEnergy) * 100.0;
    }

    public synchronized void consumeEnergy(double amount) {
        currentEnergy = Math.max(0, currentEnergy - amount);
        if (currentEnergy <= 0) die();
    }

    public void addEnergy(double amount) {
        currentEnergy = Math.min(maxEnergy, currentEnergy + amount);
    }

    protected void ageOneTick() {
        age++;
        if (maxLifespan > 0 && age >= maxLifespan) die();
    }

    @Override
    public void checkState() {
        ageOneTick();
        consumeEnergy(maxEnergy * BASE_METABOLISM_PERCENT);
    }

    public abstract String getSpeciesKey();
}
