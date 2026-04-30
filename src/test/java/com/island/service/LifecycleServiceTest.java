package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.DeathCause;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LifecycleServiceTest {

    private LifecycleService lifecycleService;

    @Mock
    private SimulationWorld world;

    @Mock
    private RandomProvider random;

    @Mock
    private Animal animal;

    private Cell cell;
    private SpeciesRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader().load();
        lifecycleService = new LifecycleService(world, Executors.newSingleThreadExecutor(), random);
        cell = new Cell(0, 0, world);
        
        // Mocking getParallelWorkUnits to return our test cell
        List<SimulationNode> workUnit = Collections.singletonList(cell);
        Collection<List<SimulationNode>> workUnits = Collections.singletonList(workUnit);
        given(world.getParallelWorkUnits()).willReturn((Collection) workUnits);

        // Required for cell.addAnimal
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        given(animal.getAnimalType()).willReturn(wolfType);
        given(animal.getMaxPerCell()).willReturn(wolfType.getMaxPerCell());
        given(animal.getSpeciesKey()).willReturn(SpeciesKey.WOLF);
    }

    @Test
    @DisplayName("Animal should survive and consume energy when metabolism is met")
    void should_survive_and_consume_energy_when_metabolism_met() {
        // Given
        long metabolismRate = 1000L;
        
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(metabolismRate);
        given(animal.tryConsumeEnergy(metabolismRate)).willReturn(true);
        
        cell.addAnimal(animal);

        // When
        lifecycleService.tick(0);

        // Then
        verify(animal).tryConsumeEnergy(metabolismRate);
        verify(world, never()).reportDeath(any(), any());
    }

    @Test
    @DisplayName("Animal should die from starvation when it cannot consume enough energy for metabolism")
    void should_die_from_starvation_when_energy_insufficient() {
        // Given
        long metabolismRate = 5000L;
        
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(metabolismRate);
        given(animal.tryConsumeEnergy(metabolismRate)).willReturn(false); 
        
        cell.addAnimal(animal);

        // When
        lifecycleService.tick(0);

        // Then
        verify(animal).tryConsumeEnergy(metabolismRate);
        
        ArgumentCaptor<SpeciesKey> speciesCaptor = ArgumentCaptor.forClass(SpeciesKey.class);
        ArgumentCaptor<DeathCause> causeCaptor = ArgumentCaptor.forClass(DeathCause.class);
        verify(world).reportDeath(speciesCaptor.capture(), causeCaptor.capture());
        
        assertEquals(SpeciesKey.WOLF, speciesCaptor.getValue());
        assertEquals(DeathCause.HUNGER, causeCaptor.getValue());
    }
    
    @Test
    @DisplayName("Dead animals should be ignored by the lifecycle process")
    void should_ignore_already_dead_animals() {
        // Given
        given(animal.isAlive()).willReturn(false);
        cell.addAnimal(animal);

        // When
        lifecycleService.tick(0);

        // Then
        verify(animal, never()).tryConsumeEnergy(anyLong());
        verify(animal, never()).getDynamicMetabolismRate();
        verify(world, never()).reportDeath(any(), any());
    }
}
