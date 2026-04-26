package com.island.content;

import lombok.Getter;
import static com.island.config.SimulationConstants.*;

/**
 * Base class for all animals in the simulation.
 */
public abstract class Animal extends Organism implements Mobile, Consumer, Reproducible<Animal> {
    protected final AnimalType animalType; 

    protected Animal(AnimalType animalType) {
        super(animalType.getMaxEnergy(), animalType.getMaxLifespan());
        this.animalType = animalType;
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    public AnimalType getAnimalType() { return animalType; }

    public boolean canInitiateReproduction() {
        return isAlive() && getEnergyPercentage() >= REPRODUCTION_MIN_ENERGY_PERCENT;
    }

    public boolean isProtected(int currentTick) {
        return isHiding;
    }

    public double getWeight() { return animalType.getWeight(); }
    public int getMaxPerCell() { return animalType.getMaxPerCell(); }
    public int getSpeed() { return animalType.getSpeed(); }
    public double getFoodForSaturation() { return animalType.getFoodForSaturation(); }

    @Override
    public double eat() { return 0; }

    @Override
    public boolean move() {
        if (!canPerformAction()) return false;
        double moveCost = getMaxEnergy() * (BASE_MOVE_COST_PERCENT 
            + (getSpeed() * SPEED_MOVE_COST_STEP_PERCENT));
        consumeEnergy(moveCost);
        return true;
    }

    public boolean trySpendEnergyForReproduction() {
        if (getEnergyPercentage() < REPRODUCTION_MIN_ENERGY_PERCENT) return false;
        double cost = getMaxEnergy() * REPRODUCTION_COST_PERCENT;
        if (getCurrentEnergy() > cost) {
            consumeEnergy(cost);
            return true;
        }
        return false;
    }

    @Override
    public abstract Animal reproduce();

    public boolean canEat(String preyKey) { return animalType.canEat(preyKey); }
    public int getHuntProbability(String preyKey) { return animalType.getHuntProbability(preyKey); }

    public boolean isAnimalPredator() {
        return animalType.isPredator();
    }
}
