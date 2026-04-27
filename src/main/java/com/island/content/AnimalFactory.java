package com.island.content;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.island.content.animals.predators.*;
import com.island.content.animals.herbivores.*;

import java.util.function.Function;

public final class AnimalFactory {
    private final SpeciesConfig config;
    private final Map<SpeciesKey, Function<AnimalType, Animal>> registry = new EnumMap<>(SpeciesKey.class);

    public AnimalFactory(SpeciesConfig config) {
        this.config = config;
        registry.put(SpeciesKey.WOLF, Wolf::new);
        registry.put(SpeciesKey.RABBIT, Rabbit::new);
        registry.put(SpeciesKey.DUCK, Duck::new);
        registry.put(SpeciesKey.FOX, Fox::new);
        registry.put(SpeciesKey.BOA, Boa::new);
        registry.put(SpeciesKey.BEAR, Bear::new);
        registry.put(SpeciesKey.EAGLE, Eagle::new);
        registry.put(SpeciesKey.HORSE, Horse::new);
        registry.put(SpeciesKey.DEER, Deer::new);
        registry.put(SpeciesKey.MOUSE, Mouse::new);
        registry.put(SpeciesKey.GOAT, Goat::new);
        registry.put(SpeciesKey.SHEEP, Sheep::new);
        registry.put(SpeciesKey.BOAR, Boar::new);
        registry.put(SpeciesKey.BUFFALO, Buffalo::new);
    }

    public Optional<Animal> createAnimal(SpeciesKey key) {
        return createAnimal(key, 0.5); 
    }

    public Optional<Animal> createAnimalWithEnergy(SpeciesKey key, double energy) {
        Optional<Animal> animalOpt = createAnimal(key, 1.0);
        animalOpt.ifPresent(a -> a.setEnergy(energy));
        return animalOpt;
    }

    public Optional<Animal> createBaby(SpeciesKey key) {
        return createAnimal(key, 0.3); 
    }

    private Optional<Animal> createAnimal(SpeciesKey key, double energyFactor) {
        Function<AnimalType, Animal> creator = registry.get(key);
        if (creator == null) return Optional.empty();
        
        AnimalType type = config.getAnimalType(key);
        if (type == null) return Optional.empty();

        Animal animal = creator.apply(type);
        animal.setEnergyFactor(energyFactor);
        return Optional.of(animal);
    }

    public Set<SpeciesKey> getRegisteredSpecies() { return registry.keySet(); }

    // Transitional methods
    public Animal createAnimal(String key) {
        try {
            return createAnimal(SpeciesKey.fromCode(key)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
