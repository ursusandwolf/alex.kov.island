package com.island.nature.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.DefaultHuntingStrategy;
import com.island.nature.entities.HuntingStrategy;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.event.DefaultEventBus;
import com.island.nature.model.Cell;
import com.island.util.InteractionMatrix;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import java.util.Collections;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedingMechanicsTest {

    private FeedingService feedingService;
    private SpeciesRegistry registry;
    private InteractionProvider matrix;
    private AnimalFactory animalFactory;
    private final Configuration config = new Configuration();

    @Mock
    private NatureWorld world;

    @Mock
    private RandomProvider random;

    private Cell cell;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader(config).load();
        matrix = InteractionMatrix.buildFrom(registry);
        animalFactory = new AnimalFactory(registry, random);
        HuntingStrategy strategy = new DefaultHuntingStrategy(config, matrix);
        
        given(world.getConfiguration()).willReturn(config);
        feedingService = new FeedingService(world, animalFactory, matrix, registry, strategy, Executors.newSingleThreadExecutor(), random, new DefaultEventBus());
        cell = new Cell(0, 0, world);
        
        given(world.getRegistry()).willReturn(registry);
        given(world.getProtectionMap()).willReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("Predator should hunt successfully based on matrix chance")
    void predator_should_hunt_successfully() {
        Animal wolf = animalFactory.createInitialAnimal(SpeciesKey.WOLF).orElseThrow();
        Animal rabbit = animalFactory.createInitialAnimal(SpeciesKey.RABBIT).orElseThrow();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        // Force success
        given(random.nextInt(0, 100)).willReturn(0); 
        
        feedingService.processCell(cell, 1);
        
        assertTrue(rabbit.isAlive() == false || cell.getAnimalCount() == 1);
    }
}
