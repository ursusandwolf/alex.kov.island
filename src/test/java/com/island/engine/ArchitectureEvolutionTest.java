package com.island.engine;

import com.island.engine.ecs.Component;
import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EntityDiedEvent;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.components.HealthComponent;
import com.island.nature.entities.components.MovementComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureEvolutionTest {

    @Test
    @DisplayName("EventBus should correctly deliver EntityDiedEvent to subscribers")
    void eventBusShouldDeliverEvents() {
        EventBus eventBus = new DefaultEventBus();
        AtomicReference<EntityDiedEvent> caughtEvent = new AtomicReference<>();
        
        eventBus.subscribe(EntityDiedEvent.class, caughtEvent::set);
        
        // Mock a mortal entity
        Mortal entity = new Mortal() {
            @Override public boolean isAlive() { return true; }
            @Override public void die() {}
            @Override public String getTypeName() { return "Test"; }
        };
        EntityDiedEvent event = new EntityDiedEvent(entity, "TEST_CAUSE");
        
        eventBus.publish(event);
        
        assertNotNull(caughtEvent.get());
        assertEquals("TEST_CAUSE", caughtEvent.get().getCause());
        assertEquals(entity, caughtEvent.get().getEntity());
    }

    @Test
    @DisplayName("Organism should support dynamic components (ECS)")
    void organismShouldSupportComponents() {
        Configuration config = new Configuration();
        // Anonymous implementation of Organism for testing
        Organism organism = new Organism(config, 100, 10) {
            @Override
            public String getTypeName() { return "Test"; }
            @Override
            public SpeciesKey getSpeciesKey() { return SpeciesKey.RABBIT; }
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
                .speciesKey(SpeciesKey.RABBIT)
                .typeName("Rabbit")
                .speed(2)
                .maxEnergy(100)
                .maxLifespan(10)
                .huntProbabilities(new HashMap<>())
                .build();

        Animal rabbit = new Animal(rabbitType) {};
        
        MovementComponent move = rabbit.getComponent(MovementComponent.class);
        assertNotNull(move);
        assertEquals(2, move.getSpeed());
        
        // Test speed sync
        rabbit.mutate(1.0, 5);
        assertEquals(7, move.getSpeed());
    }
}
