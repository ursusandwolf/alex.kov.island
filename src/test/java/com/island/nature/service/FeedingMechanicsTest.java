package com.island.nature.service;

import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.DefaultHuntingStrategy;
import com.island.nature.entities.strategy.HuntingStrategy;
import com.island.util.common.RandomProvider;
import com.island.util.interaction.InteractionMatrix;
import com.island.util.interaction.InteractionProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedingMechanicsTest {

    private AnimalFeedingSystem feedingSystem;
    private SpeciesRegistry registry;
    private InteractionProvider matrix;
    private AnimalFactory animalFactory;
    private final Configuration config = new Configuration();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Mock
    private NatureWorld world;

    @Mock
    private RandomProvider random;

    private Cell cell;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader(config).load();
        matrix = InteractionMatrix.buildFrom(registry);
        animalFactory = new AnimalFactory(registry, random, new com.island.engine.ecs.ComponentRegistry());
        HuntingStrategy strategy = new DefaultHuntingStrategy(config, matrix);
        
        given(world.getConfiguration()).willReturn(config);
        feedingSystem = new AnimalFeedingSystem(world, animalFactory, matrix, registry, strategy, executor, random);
        cell = new Cell(0, 0, world);
        
        given(world.getRegistry()).willReturn(registry);
        given(world.getProtectionMap()).willReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("Predator should hunt successfully based on matrix chance")
    void predator_should_hunt_successfully() {
        Animal wolf = animalFactory.createInitialAnimal(new SpeciesKey("wolf", true)).orElseThrow();
        Animal rabbit = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        // Force success
        given(random.nextInt(0, 100)).willReturn(0); 
        
        feedingSystem.processCell(cell, 1);
        
        assertTrue(rabbit.isAlive() == false || cell.getAnimalCount() == 1);
    }
}
