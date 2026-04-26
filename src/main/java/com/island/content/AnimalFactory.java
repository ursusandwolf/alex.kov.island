package com.island.content;

import java.util.*;
import com.island.content.animals.predators.*;
import com.island.content.animals.herbivores.*;

import java.util.function.Function;

public final class AnimalFactory {
    private final SpeciesConfig config;
    private final Map<String, Function<AnimalType, Animal>> registry = new HashMap<>();

    public AnimalFactory(SpeciesConfig config) {
        this.config = config;
        registry.put("wolf", Wolf::new);
        registry.put("rabbit", Rabbit::new);
        registry.put("duck", Duck::new);
        registry.put("caterpillar", Caterpillar::new);
        registry.put("fox", Fox::new);
        registry.put("boa", Boa::new);
        registry.put("bear", Bear::new);
        registry.put("eagle", Eagle::new);
        registry.put("horse", Horse::new);
        registry.put("deer", Deer::new);
        registry.put("mouse", Mouse::new);
        registry.put("goat", Goat::new);
        registry.put("sheep", Sheep::new);
        registry.put("boar", Boar::new);
        registry.put("buffalo", Buffalo::new);
    }

    public Animal createAnimal(String key) {
        return createAnimal(key, 0.5); 
    }

    public Animal createAnimalWithEnergy(String key, double energy) {
        Animal animal = createAnimal(key, 1.0); // Create with max energy base
        if (animal != null) {
            animal.setEnergy(energy);
        }
        return animal;
    }

    public Animal createBaby(String key) {
        return createAnimal(key, 0.3); 
    }

    private Animal createAnimal(String key, double energyFactor) {
        Function<AnimalType, Animal> creator = registry.get(key.toLowerCase());
        if (creator == null) return null;
        
        AnimalType type = config.getAnimalType(key);
        if (type == null) return null;

        Animal animal = creator.apply(type);
        animal.setEnergyFactor(energyFactor);
        return animal;
    }

    public Set<String> getRegisteredSpecies() { return registry.keySet(); }
}
