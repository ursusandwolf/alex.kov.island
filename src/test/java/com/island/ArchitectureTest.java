package com.island;

import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import com.island.nature.entities.components.HealthComponent;
import com.island.nature.entities.components.MovementComponent;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.island");

    @Test
    void engineShouldNotDependOnDomainPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..engine..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..nature..", "..simcity..");

        rule.check(importedClasses);
    }

    @Test
    void utilShouldNotDependOnDomainPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..util..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..nature..", "..simcity..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("ECS: Organism should support dynamic components")
    void organismShouldSupportComponents() {
        Configuration config = new Configuration();
        ComponentRegistry registry = new ComponentRegistry();
        Organism organism = new Organism(config, registry, 100, 10) {
            @Override public String getTypeName() { return "Test"; }
            @Override public SpeciesKey getSpeciesKey() { return new SpeciesKey("rabbit", false); }
        };

        assertNotNull(organism.getComponent(HealthComponent.class));

        @AllArgsConstructor @Getter
        class CustomComponent implements Component { String data; }

        organism.addComponent(new CustomComponent("secret"));
        assertNotNull(organism.getComponent(CustomComponent.class));
        assertEquals("secret", organism.getComponent(CustomComponent.class).getData());
    }

    @Test
    @DisplayName("ECS: Animal should have MovementComponent and sync speed")
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

        rabbit.mutate(1.0, 5); // Speed bonus
        assertEquals(7, move.getSpeed());
    }
}