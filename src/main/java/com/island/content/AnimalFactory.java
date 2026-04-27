package com.island.content;

import com.island.content.animals.herbivores.Boar;
import com.island.content.animals.herbivores.Buffalo;
import com.island.content.animals.herbivores.Deer;
import com.island.content.animals.herbivores.Duck;
import com.island.content.animals.herbivores.Goat;
import com.island.content.animals.herbivores.Hamster;
import com.island.content.animals.herbivores.Horse;
import com.island.content.animals.herbivores.Mouse;
import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.herbivores.Sheep;
import com.island.content.animals.predators.Bear;
import com.island.content.animals.predators.Boa;
import com.island.content.animals.predators.Eagle;
import com.island.content.animals.predators.Fox;
import com.island.content.animals.predators.Wolf;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory for creating animal instances based on SpeciesKey.
 */
public final class AnimalFactory {
    private final SpeciesRegistry speciesRegistry;
    private final Map<SpeciesKey, Function<AnimalType, Animal>> creators = new EnumMap<>(SpeciesKey.class);

    public AnimalFactory(SpeciesRegistry speciesRegistry) {
        this.speciesRegistry = speciesRegistry;
        creators.put(SpeciesKey.WOLF, Wolf::new);
        creators.put(SpeciesKey.RABBIT, Rabbit::new);
        creators.put(SpeciesKey.DUCK, Duck::new);
        creators.put(SpeciesKey.FOX, Fox::new);
        creators.put(SpeciesKey.BOA, Boa::new);
        creators.put(SpeciesKey.BEAR, Bear::new);
        creators.put(SpeciesKey.EAGLE, Eagle::new);
        creators.put(SpeciesKey.HORSE, Horse::new);
        creators.put(SpeciesKey.DEER, Deer::new);
        creators.put(SpeciesKey.MOUSE, Mouse::new);
        creators.put(SpeciesKey.HAMSTER, Hamster::new);
        creators.put(SpeciesKey.GOAT, Goat::new);
        creators.put(SpeciesKey.SHEEP, Sheep::new);
        creators.put(SpeciesKey.BOAR, Boar::new);
        creators.put(SpeciesKey.BUFFALO, Buffalo::new);
    }

    public Optional<Animal> createAnimal(SpeciesKey key) {
        return createAnimal(key, 0.5); 
    }

    /**
     * Transitional methods.
     */
    public Animal createAnimal(String key) {
        try {
            return createAnimal(SpeciesKey.fromCode(key)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Optional<Animal> createAnimal(SpeciesKey key, double energyFactor) {
        Function<AnimalType, Animal> creator = creators.get(key);
        if (creator == null) {
            return Optional.empty();
        }
        
        AnimalType type = speciesRegistry.getAnimalType(key).orElse(null);
        if (type == null) {
            return Optional.empty();
        }

        Animal animal = creator.apply(type);
        animal.setEnergyFactor(energyFactor);
        return Optional.of(animal);
    }

    public Optional<Animal> createBaby(SpeciesKey key) {
        return createAnimal(key, 0.3); 
    }

    public Optional<Animal> createAnimalWithEnergy(SpeciesKey key, double energy) {
        Optional<Animal> animalOpt = createAnimal(key, 1.0);
        animalOpt.ifPresent(a -> a.setEnergy(energy));
        return animalOpt;
    }

    public Set<SpeciesKey> getRegisteredSpecies() {
        return creators.keySet();
    }
}
