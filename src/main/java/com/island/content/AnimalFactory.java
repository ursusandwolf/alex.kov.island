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
        Function<AnimalType, Animal> creator = registry.get(key.toLowerCase());
        if (creator == null) return null;
        
        AnimalType type = config.getAnimalType(key);
        return (type != null) ? creator.apply(type) : null;
    }

    public Set<String> getRegisteredSpecies() { return registry.keySet(); }
}
