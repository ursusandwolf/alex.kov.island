package com.island.content;

import com.island.content.animals.predators.Bear;
import com.island.content.animals.predators.Chameleon;
import com.island.util.ObjectPool;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory for creating animal instances based on SpeciesKey.
 * Enhanced with Object Pooling to reduce GC pressure.
 */
public final class AnimalFactory {
    private final SpeciesRegistry speciesRegistry;
    private final com.island.util.RandomProvider random;
    private final Map<SpeciesKey, Function<AnimalType, Animal>> creators = new HashMap<>();
    private final Map<SpeciesKey, ObjectPool<Animal>> pools = new HashMap<>();

    public AnimalFactory(SpeciesRegistry speciesRegistry, com.island.util.RandomProvider random) {
        this.speciesRegistry = speciesRegistry;
        this.random = random;
        // Only register animals with unique logic here. 
        // All others will be created as GenericAnimal.
        creators.put(SpeciesKey.BEAR, Bear::new);
        creators.put(SpeciesKey.CHAMELEON, Chameleon::new);
        
        // Initialize pools for all animal species
        for (SpeciesKey key : speciesRegistry.getAllAnimalKeys()) {
            pools.put(key, new ObjectPool<>(() -> createNewAnimal(key)));
        }
    }

    private Animal createNewAnimal(SpeciesKey key) {
        AnimalType type = speciesRegistry.getAnimalType(key).orElseThrow();
        Function<AnimalType, Animal> creator = creators.get(key);
        return (creator != null) ? creator.apply(type) : new GenericAnimal(type);
    }

    public Animal createAnimal(String key) {
        try {
            return createAnimal(SpeciesKey.fromCode(key), 0.5, 0).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Optional<Animal> createAnimal(SpeciesKey key) {
        return createAnimal(key, 0.5, 0); 
    }

    private Optional<Animal> createAnimal(SpeciesKey key, double energyFactor, int initialAge) {
        ObjectPool<Animal> pool = pools.get(key);
        if (pool == null) {
            return Optional.empty();
        }

        AnimalType type = speciesRegistry.getAnimalType(key).orElse(null);
        if (type == null) {
            return Optional.empty();
        }

        Animal animal = pool.acquire();
        animal.init(type, energyFactor);
        
        for (int i = 0; i < initialAge; i++) {
            animal.checkAgeDeath(); // Increment age
        }
        return Optional.of(animal);
    }

    public void releaseAnimal(Animal animal) {
        if (animal != null) {
            ObjectPool<Animal> pool = pools.get(animal.getSpeciesKey());
            if (pool != null) {
                pool.release(animal);
            }
        }
    }

    public Optional<Animal> createInitialAnimal(SpeciesKey key) {
        // Randomize initial age (at most 5% of max lifespan) and energy for a more natural start
        AnimalType type = speciesRegistry.getAnimalType(key).orElse(null);
        int initialAge = 0;
        if (type != null && type.getMaxLifespan() > 0) {
            int maxInitialAge = Math.max(1, (int) (type.getMaxLifespan() * 0.05));
            initialAge = random.nextInt(0, maxInitialAge);
        }
        double initialEnergyFactor = 0.4 + (random.nextDouble() * 0.4);
        return createAnimal(key, initialEnergyFactor, initialAge);
    }

    public Optional<Animal> createBaby(SpeciesKey key) {
        return createAnimal(key, 0.3, 0); 
    }

    public Optional<Animal> createAnimalWithEnergy(SpeciesKey key, double energy) {
        Optional<Animal> animalOpt = createAnimal(key, 1.0, 0);
        animalOpt.ifPresent(a -> a.setEnergy(energy));
        return animalOpt;
    }

    public Set<SpeciesKey> getRegisteredSpecies() {
        return speciesRegistry.getAllAnimalKeys();
    }
}
