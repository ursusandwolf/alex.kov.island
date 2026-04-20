package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.Predator;
import com.island.content.SpeciesConfig;

//todo improve flyweight optimization of Animals
public class Fox extends Animal implements Predator {
    
    public Fox() {
        super(
                8.0,      // weight kg
                30,        // max per cell
                2,         // speed
                2.0,       // food for saturation kg
                10000      // max lifespan ticks
        );
    }

    @Override
    public String getTypeName() {
        return "Fox";
    }

    @Override
    public String getSpeciesKey() {
        return "fox";
    }
    
    @Override
    public boolean canEat(String preySpeciesKey) {
        return SpeciesConfig.getInstance().canEat("fox", preySpeciesKey);
    }
    
    @Override
    public int getHuntProbability(String preySpeciesKey) {
        return SpeciesConfig.getInstance().getHuntProbability("fox", preySpeciesKey);
    }
    
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }

        System.out.println("Fox " + getId().substring(0, 8) + " is looking for prey...");
        return 0; // Placeholder - needs Cell reference to implement hunting
    }

    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }

        return super.move();
    }

    @Override
    public Fox reproduce() {
        if (!canPerformAction()) {
            return null;
        }

        System.out.println("Fox " + getId().substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder - needs Cell reference to find mate
    }
}
