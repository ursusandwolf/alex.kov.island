package com.island;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.components.HealthComponent;
import com.island.nature.entities.components.MovementComponent;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;

class ArchitectureEvolutionTest {

    @Test
    @DisplayName("Organism should support dynamic components (ECS)")
    void organismShouldSupportComponents() {
        Configuration config = new Configuration();
        ComponentRegistry registry = new ComponentRegistry();
        // Anonymous implementation of Organism for testing
        Organism organism = new Organism(config, registry, 100, 10) {
            @Override
            public String getTypeName() { return "Test"; }
            @Override
            public SpeciesKey getSpeciesKey() { return new SpeciesKey("rabbit", false); }
        };

        // Test built-in components
        assertNotNull(organism.getComponent(HealthComponent.class));

        // Test adding a custom component
        @AllArgsConstructor
        @Getter
        class CustomComponent implements Component {
            String data;
        }
        
        organism.addComponent(new CustomComponent("secret"));
        
        CustomComponent comp = organism.getComponent(CustomComponent.class);
        assertNotNull(comp);
        assertEquals("secret", comp.getData());
    }

    @Test
    @DisplayName("Animal should have MovementComponent by default")
    void animalShouldHaveMovementComponent() {
        AnimalType rabbitType = AnimalType.builder()
                .config(new Configuration())
                .speciesKey(new SpeciesKey("rabbit", false))
                .typeName("Rabbit")
                .speed(2)
                .maxEnergy(100)
                .maxLifespan(10)
                .huntProbabilities(new HashMap<>())
                .build();

        Animal rabbit = new Animal(rabbitType, new ComponentRegistry()) {};
        
        MovementComponent move = rabbit.getComponent(MovementComponent.class);
        assertNotNull(move);
        assertEquals(2, move.getSpeed());
        
        // Test speed sync
        rabbit.mutate(1.0, 5);
        assertEquals(7, move.getSpeed());
    }
}