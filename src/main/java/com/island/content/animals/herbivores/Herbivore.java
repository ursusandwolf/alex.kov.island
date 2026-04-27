package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

/**
 * Interface/Base for herbivores.
 */
public abstract class Herbivore extends Animal {
    public Herbivore(AnimalType type) {
        super(type);
    }
}
