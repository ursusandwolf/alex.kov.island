package com.island.content;

import lombok.Getter;

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
    public double eat() {
        System.out.println(getTypeName() + " требуется реализация eat()");
        return 0;
    }

    @Override
    public boolean move() {
        if (!canPerformAction()) return false;
        consumeEnergy(getMaxEnergy() * 0.05); // 5% затрат
        return false;
    }

    @Override
    public Animal reproduce() {
        if (!canPerformAction()) return null;
        consumeEnergy(getMaxEnergy() * 0.05); // 5% затрат
        return null;
    }

    public boolean canEat(String preyKey) { return animalType.canEat(preyKey); }
    public int getHuntProbability(String preyKey) { return animalType.getHuntProbability(preyKey); }
}
