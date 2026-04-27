package com.island.content;

import static com.island.config.SimulationConstants.BASE_MOVE_COST_PERCENT;
import static com.island.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER;
import static com.island.config.SimulationConstants.REPRODUCTION_COST_PERCENT;
import static com.island.config.SimulationConstants.REPRODUCTION_MIN_ENERGY_PERCENT;
import static com.island.config.SimulationConstants.SPEED_MOVE_COST_STEP_PERCENT;

import com.island.content.plants.Plant;

/**
 * Base class for all animals in the simulation.
 * Represents the state and properties of an animal.
 */
public abstract class Animal extends Organism {
    protected final AnimalType animalType; 
    protected boolean isHiding = false;

    protected Animal(AnimalType animalType) {
        super(animalType.getMaxEnergy(), animalType.getMaxLifespan());
        this.animalType = animalType;
    }

    @Override
    public String getTypeName() {
        return animalType.getTypeName();
    }

    @Override
    public SpeciesKey getSpeciesKey() {
        return animalType.getSpeciesKey();
    }

    public AnimalType getAnimalType() {
        return animalType;
    }

    public boolean isHiding() {
        return isHiding;
    }

    public void setHiding(boolean h) {
        this.isHiding = h;
    }

    public boolean canInitiateReproduction() {
        return isAlive() && getEnergyPercentage() >= REPRODUCTION_MIN_ENERGY_PERCENT;
    }

    public boolean isProtected(int currentTick) {
        return isHiding;
    }

    @Override
    public double getWeight() {
        return animalType.getWeight();
    }

    public int getMaxPerCell() {
        return animalType.getMaxPerCell();
    }

    public int getSpeed() {
        return animalType.getSpeed();
    }

    public double getFoodForSaturation() {
        return animalType.getFoodForSaturation();
    }

    @Override
    public double getDynamicMetabolismRate() {
        double rate = super.getDynamicMetabolismRate();
        // Herbivores get a survival bonus
        if (this instanceof com.island.content.animals.herbivores.Herbivore) {
            rate *= HERBIVORE_METABOLISM_MODIFIER;
        }
        return rate;
    }

    public boolean canEat(SpeciesKey preyKey) {
        return animalType.canEat(preyKey);
    }

    public int getHuntProbability(SpeciesKey preyKey) {
        return animalType.getHuntProbability(preyKey);
    }

    public boolean isAnimalPredator() {
        return animalType.isPredator();
    }

    public boolean isHibernating() {
        return false;
    }
}
