package com.island.content;

import lombok.Getter;
import static com.island.config.SimulationConstants.*;

// Базовый класс животных (Flyweight для AnimalType)
@Getter
public abstract class Animal extends Organism {
    protected final AnimalType animalType; // Общие данные вида (Flyweight)
    private volatile boolean isHiding = false;

    protected Animal(AnimalType animalType) {
        super(animalType.getMaxEnergy(), animalType.getMaxLifespan());
        this.animalType = animalType;
    }

    public void setHiding(boolean hiding) { this.isHiding = hiding; }

    public boolean isProtected(int currentTick) {
        return isHiding;
    }

    public double getWeight() { return animalType.getWeight(); }
    public int getMaxPerCell() { return animalType.getMaxPerCell(); }
    public int getSpeed() { return animalType.getSpeed(); }
    public double getFoodForSaturation() { return animalType.getFoodForSaturation(); }

    @Override
    public double eat() {
        // Default implementation for animals. 
        // Actual feeding logic is handled by FeedingService.
        return 0;
    }

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
        // Reproduction is expensive: 30% of max energy
        double cost = getMaxEnergy() * REPRODUCTION_COST_PERCENT;
        if (getCurrentEnergy() > cost) {
            consumeEnergy(cost);
            return AnimalFactory.createAnimal(getSpeciesKey());
        }
        return null;
    }

    public boolean canEat(String preyKey) { return animalType.canEat(preyKey); }
    public int getHuntProbability(String preyKey) { return animalType.getHuntProbability(preyKey); }

    /**
     * Checks if this animal eats other animals (anything except plants).
     */
    public boolean isAnimalPredator() {
        return animalType.isPredator();
    }
}
