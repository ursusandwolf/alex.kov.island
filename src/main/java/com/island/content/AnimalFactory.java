package com.island.content;

import java.util.*;
import com.island.content.animals.predators.*;
import com.island.content.animals.herbivores.*;

public final class AnimalFactory {
    @FunctionalInterface
    public interface AnimalCreator { Animal create(); }

    private static final Map<String, AnimalCreator> registry = new HashMap<>();

    static {
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

    private AnimalFactory() {}

    public static Animal createAnimal(String key) {
        AnimalCreator creator = registry.get(key.toLowerCase());
        if (creator == null) return null;
        return creator.create();
    }

    public static Set<String> getRegisteredSpecies() { return registry.keySet(); }
}
