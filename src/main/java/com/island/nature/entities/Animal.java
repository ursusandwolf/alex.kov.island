package com.island.nature.entities;

import static com.island.nature.config.SimulationConstants.SCALE_10K;

import com.island.nature.config.EnergyPolicy;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Animal extends Organism {
    protected final AnimalType animalType; 
    @Setter protected boolean hiding = false;
    protected long weightOverride = 0;
    protected int speedOverride = -1;

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

    public void init(AnimalType type, int energyPercent) {
        super.init(type.getMaxEnergy(), type.getMaxLifespan(), energyPercent);
        this.hiding = false;
        this.weightOverride = 0;
        this.speedOverride = -1;
    }

    public void mutate(double weightFactor, int speedDelta) {
        this.weightOverride = (long) (getWeight() * weightFactor);
        this.speedOverride = Math.max(0, getSpeed() + speedDelta);
    }

    public boolean canInitiateReproduction() {
        return isAlive() && getAge() >= 1 && getEnergyPercentage() >= EnergyPolicy.REPRODUCTION_MIN.getPercent();
    }

    public boolean isProtected(int currentTick) {
        return hiding;
    }

    @Override
    public long getWeight() {
        return weightOverride > 0 ? weightOverride : animalType.getWeight();
    }

    public int getMaxPerCell() {
        return animalType.getMaxPerCell();
    }

    public int getSpeed() {
        return speedOverride >= 0 ? speedOverride : animalType.getSpeed();
    }

    public long getFoodForSaturation() {
        return animalType.getFoodForSaturation();
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

    @Override
    protected int getSpecialMetabolismModifierBP() {
        return SCALE_10K;
    }

    public int getOffspringBonus() {
        return 0;
    }
}
