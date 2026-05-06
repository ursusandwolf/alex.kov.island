package com.island.nature.entities.core;

import com.island.nature.entities.herbivores.Herbivore;
import com.island.nature.entities.predators.Predator;

/**
 * A generic animal implementation that uses AnimalType for all its properties.
 * Uses integer-based arithmetic for metabolism.
 */
public class GenericAnimal extends Animal implements Herbivore, Predator {
    private final boolean isHerbivore;
    private final boolean isColdBlooded;

    public GenericAnimal(AnimalType type) {
        super(type);
        // If an animal can eat plants, we treat it as a herbivore for metabolic bonuses
        this.isHerbivore = type.getPreySpecies().stream()
                .anyMatch(k -> {
                    String code = k.getCode();
                    return "plant".equals(code) || "grass".equals(code) || "mushroom".equals(code);
                });
        this.isColdBlooded = type.isColdBlooded();
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        int modifier = isHerbivore ? config.getHerbivoreMetabolismModifierBP() : config.getScale10K();
        if (isColdBlooded) {
            modifier = (modifier * config.getReptileMetabolismModifierBP()) / config.getScale10K();
        }
        return modifier;
    }

    @Override
    public int getHerbivoreMetabolismModifierBP() {
        return config.getHerbivoreMetabolismModifierBP();
    }

    @Override
    public int getHerbivoreOffspringBonus() {
        return config.getHerbivoreOffspringBonus();
    }

    @Override
    public int getPredatorMetabolismModifierBP() {
        return config.getScale10K();
    }

    @Override
    public int getOffspringBonus() {
        return isHerbivore ? config.getHerbivoreOffspringBonus() : 0;
    }
}