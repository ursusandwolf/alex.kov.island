package com.island.content;

import java.util.*;
import com.island.content.animals.*;

// Фабрика для создания животных (Factory Method)
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
    }

    private AnimalFactory() {}

    public static void register(String key, AnimalCreator creator) { registry.put(key, creator); }

    public static Animal createAnimal(String key) {
        AnimalCreator creator = registry.get(key);
        if (creator == null) System.err.println("Неизвестный вид: " + key);
        return (creator != null) ? creator.create() : null;
    }

    public static boolean isRegistered(String key) { return registry.containsKey(key); }
    public static Set<String> getRegisteredSpecies() { return registry.keySet(); }
}
