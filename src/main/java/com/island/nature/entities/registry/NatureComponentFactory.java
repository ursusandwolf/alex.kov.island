package com.island.nature.entities.registry;

import com.island.engine.ecs.Component;
import com.island.nature.entities.components.ConsumableComponent;
import com.island.nature.entities.components.GrowthComponent;
import com.island.nature.entities.components.MetabolismComponent;
import com.island.nature.entities.components.MovementComponent;
import com.island.nature.entities.components.ReproductionComponent;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.DeathCause;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating nature-specific component bundles.
 */
public class NatureComponentFactory {

    public List<Component> createAnimalComponents(AnimalType type, Animal animal) {
        List<Component> components = new ArrayList<>();
        components.add(new MovementComponent(type.getSpeed()));
        
        // Base metabolism calculation moved to component initialization
        long baseMetabolism = (type.getMaxEnergy() * animal.getConfig().getBaseMetabolismBP()) / animal.getConfig().getScale10K();
        components.add(new MetabolismComponent(baseMetabolism));
        
        components.add(new ReproductionComponent(type.getReproductionChance(), type.getMaxOffspring()));
        
        components.add(ConsumableComponent.builder()
                .isAnimal(true)
                .consumeAction(requested -> {
                    if (animal.isAlive()) {
                        animal.die(DeathCause.EATEN);
                        return animal.getWeight();
                    }
                    return 0;
                })
                .build());
                
        return components;
    }

    public List<Component> createBiomassComponents(Biomass biomass) {
        List<Component> components = new ArrayList<>();
        if (biomass.getSpeed() > 0) {
            components.add(new MovementComponent(biomass.getSpeed()));
        }
        
        components.add(new GrowthComponent(biomass.getConfig().getPlantGrowthRateBP(), biomass.getMaxBiomass()));
        
        components.add(ConsumableComponent.builder()
                .isAnimal(false)
                .consumeAction(requested -> {
                    // This is slightly tricky as consumeBiomass needs Cell context in current implementation.
                    // For ECS System, the system itself will have the Cell context.
                    // So we might need to pass the Cell to the consume method.
                    return 0; // Systems should handle biomass consumption directly or via enhanced component
                })
                .build());
                
        return components;
    }
}
