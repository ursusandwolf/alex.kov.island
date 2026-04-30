package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.Biomass;
import com.island.content.DefaultHuntingStrategy;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.content.plants.Grass;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.model.Cell;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedingMechanicsTest {

    private FeedingService feedingService;
    private AnimalFactory animalFactory;
    
    @Mock
    private SimulationWorld world;
    
    private Cell cell;
    private SpeciesRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader().load();
        cell = new Cell(0, 0, world);
        
        List<SimulationNode> workUnit = Collections.singletonList(cell);
        Collection<List<SimulationNode>> workUnits = Collections.singletonList(workUnit);
        
        given(world.getParallelWorkUnits()).willReturn((Collection) workUnits);
        given(world.getProtectionMap(registry)).willReturn(Collections.emptyMap());

        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);
        
        RandomProvider deterministicRandom = new RandomProvider() {
            @Override public int nextInt(int bound) { return 0; }
            @Override public int nextInt(int origin, int bound) { return origin; }
            @Override public long nextLong() { return 0L; }
            @Override public double nextDouble() { return 0.5; } 
            @Override public double nextDouble(double bound) { return 0.0; }
            @Override public boolean nextBoolean() { return false; }
        };

        animalFactory = new AnimalFactory(registry, deterministicRandom);
        feedingService = new FeedingService(world, animalFactory, matrix, registry,
                new DefaultHuntingStrategy(matrix), Executors.newSingleThreadExecutor(), deterministicRandom);
    }

    @Test
    @DisplayName("Predator should eat prey and increase energy")
    void should_predator_eat_prey_and_increase_energy() {
        // Given
        Animal wolf = animalFactory.createAnimal(SpeciesKey.WOLF).orElseThrow();
        Animal rabbit = animalFactory.createAnimal(SpeciesKey.RABBIT).orElseThrow();
        
        wolf.setEnergy(2.0); 
        double initialWolfEnergy = wolf.getCurrentEnergy();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);

        assertTrue(rabbit.isAlive());

        // When
        feedingService.tick(0);

        // Then
        assertTrue(wolf.getCurrentEnergy() > initialWolfEnergy, "Wolf energy should increase");
        assertFalse(cell.getAnimals().contains(rabbit), "Rabbit should be removed from cell");
    }

    @Test
    @DisplayName("Herbivore should eat plant and increase energy")
    void should_herbivore_eat_plant_and_increase_energy() {
        // Given
        Animal rabbit = animalFactory.createAnimal(SpeciesKey.RABBIT).orElseThrow();
        
        rabbit.setEnergy(0.1);
        double initialRabbitEnergy = rabbit.getCurrentEnergy();
        
        cell.addAnimal(rabbit);
        
        // Add real biomass container
        Biomass grass = new Grass(100.0, 0);
        grass.setBiomass(10.0);
        cell.addBiomass(grass);

        // When
        feedingService.tick(0);

        // Then
        assertTrue(rabbit.getCurrentEnergy() > initialRabbitEnergy, "Rabbit energy should increase");
        assertTrue(grass.getBiomass() < 10.0, "Grass biomass should decrease");
    }
}
