package com.island.content;

import com.island.content.animals.Wolf;
import com.island.content.animals.Rabbit;

/**
 * Factory for creating animal instances.
 * Uses Factory Method pattern for extensibility.
 * 
 * GOF Patterns:
 * - Factory Method: creates animals based on species key
 * - Registry: maintains mapping of species to factory functions
 * 
 * GRASP Principles:
 * - Creator: responsible for creating objects
 * - Low Coupling: animal creation is centralized
 */
public class AnimalFactory {
    
    // Functional interface for animal creation
    @FunctionalInterface
    public interface AnimalCreator {
        Animal create();
    }
    
    // Registry of animal creators
    private static final java.util.Map<String, AnimalCreator> registry = new java.util.HashMap<>();
    
    // Static initialization block
    static {
        // Register all predator species
        register("wolf", Wolf::new);
        // TODO: Register remaining predators: python, fox, bear, eagle
        
        // Register all herbivore species  
        register("rabbit", Rabbit::new);
        // TODO: Register remaining herbivores: horse, deer, mouse, goat, sheep,
        // wild_boar, buffalo, duck, caterpillar
    }
    
    /**
     * Register an animal type with its creator function.
     * 
     * @param speciesKey unique species identifier
     * @param creator lambda/function that creates the animal
     */
    public static void register(String speciesKey, AnimalCreator creator) {
        registry.put(speciesKey, creator);
    }
    
    /**
     * Create a new animal instance by species key.
     * 
     * @param speciesKey the species to create
     * @return new Animal instance or null if not found
     */
    public static Animal createAnimal(String speciesKey) {
        AnimalCreator creator = registry.get(speciesKey);
        if (creator == null) {
            System.err.println("Unknown species: " + speciesKey);
            return null;
        }
        return creator.create();
    }
    
    /**
     * Check if species is registered.
     * 
     * @param speciesKey the species
     * @return true if can be created
     */
    public static boolean isRegistered(String speciesKey) {
        return registry.containsKey(speciesKey);
    }
    
    /**
     * Get all registered species keys.
     * 
     * @return set of keys
     */
    public static java.util.Set<String> getRegisteredSpecies() {
        return registry.keySet();
    }
}
