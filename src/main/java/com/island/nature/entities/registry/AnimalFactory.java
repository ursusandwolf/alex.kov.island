package com.island.nature.entities.registry;

import com.island.nature.entities.predators.Bear;
import com.island.nature.entities.predators.Chameleon;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.GenericAnimal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.util.common.ObjectPool;
import com.island.util.common.RandomProvider;

/**
 * Factory for creating animal instances based on SpeciesKey.
 * Uses integer arithmetic for energy and age initialization.
 */
public final class AnimalFactory {
    private final SpeciesRegistry speciesRegistry;
    private final RandomProvider random;
    private final Map<SpeciesKey, Function<AnimalType, Animal>> creators = new HashMap<>();
    private final Map<SpeciesKey, ObjectPool<Animal>> pools = new HashMap<>();

    public AnimalFactory(SpeciesRegistry speciesRegistry, RandomProvider random) {
        this.speciesRegistry = speciesRegistry;
        this.random = random;
        
        speciesRegistry.getKey("bear").ifPresent(k -> creators.put(k, Bear::new));
        speciesRegistry.getKey("chameleon").ifPresent(k -> creators.put(k, type -> new Chameleon(type, random)));
        
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
        return speciesRegistry.getKey(key)
                .flatMap(this::createAnimal)
                .orElse(null);
    }

    public Optional<Animal> createAnimal(SpeciesKey key) {
        return createAnimal(key, 50, 0); 
    }

    private Optional<Animal> createAnimal(SpeciesKey key, int energyPercent, int initialAge) {
        ObjectPool<Animal> pool = pools.get(key);
        if (pool == null) {
            return Optional.empty();
        }

        AnimalType type = speciesRegistry.getAnimalType(key).orElse(null);
        if (type == null) {
            return Optional.empty();
        }

        Animal animal = pool.acquire();
        animal.init(type, energyPercent);
        
        for (int i = 0; i < initialAge; i++) {
            if (animal.checkAgeDeath()) {
                releaseAnimal(animal);
                return Optional.empty();
            }
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
        AnimalType type = speciesRegistry.getAnimalType(key).orElse(null);
        int initialAge = 0;
        if (type != null && type.getMaxLifespan() > 0) {
            // Randomize age up to 80% of lifespan to avoid "clipping" deaths
            int maxInitialAge = (type.getMaxLifespan() * 80) / 100;
            initialAge = random.nextInt(0, maxInitialAge + 1);
        }
        int initialEnergyPercent = 40 + random.nextInt(0, 41);
        return createAnimal(key, initialEnergyPercent, initialAge);
    }

    public Optional<Animal> createBaby(SpeciesKey key) {
        Optional<Animal> babyOpt = createAnimal(key, 30, 0); 
        babyOpt.ifPresent(baby -> {
            if (random.nextInt(0, 100) < 10) { // 10% mutation chance
                double weightFactor = 0.9 + (random.nextDouble() * 0.2); // 0.9 to 1.1
                int speedDelta = random.nextInt(-1, 2); // -1, 0, or 1
                baby.mutate(weightFactor, speedDelta);
            }
        });
        return babyOpt;
    }

    public Optional<Animal> createAnimalWithEnergy(SpeciesKey key, long energy) {
        Optional<Animal> animalOpt = createAnimal(key, 100, 0);
        animalOpt.ifPresent(a -> a.setEnergy(energy));
        return animalOpt;
    }

    public Set<SpeciesKey> getRegisteredSpecies() {
        return speciesRegistry.getAllAnimalKeys();
    }
}