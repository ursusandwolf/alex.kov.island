package com.island.content;

import lombok.Getter;
import static com.island.config.SimulationConstants.*;

// Базовый класс животных (Flyweight для AnimalType)
@Getter
public abstract class Animal extends Organism {
    protected final AnimalType animalType; // Общие данные вида (Flyweight)

    protected Animal(AnimalType animalType) {
        super(animalType.getMaxEnergy(), animalType.getMaxLifespan());
        this.animalType = animalType;
    }

    public double getWeight() { return animalType.getWeight(); }
    public int getMaxPerCell() { return animalType.getMaxPerCell(); }
    public int getSpeed() { return animalType.getSpeed(); }
    public double getFoodForSaturation() { return animalType.getFoodForSaturation(); }

    @Override
    public boolean move() {
        if (!canPerformAction()) return false;
        // Energy cost depends on speed
        double moveCost = getMaxEnergy() * (BASE_MOVE_COST_PERCENT 
            + (getSpeed() * SPEED_MOVE_COST_STEP_PERCENT));
        consumeEnergy(moveCost);
        return true;
    }

    @Override
    public Animal reproduce() {
        if (!canPerformAction()) return null;
        // Reproduction is expensive
        double reproductionCost = getMaxEnergy() * REPRODUCTION_COST_PERCENT;
        if (getCurrentEnergy() > reproductionCost) {
            consumeEnergy(reproductionCost);
            return AnimalFactory.createAnimal(getSpeciesKey());
        }
        return null;
    }

    public boolean canEat(String preyKey) { return animalType.canEat(preyKey); }
    public int getHuntProbability(String preyKey) { return animalType.getHuntProbability(preyKey); }
}
