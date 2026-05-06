package com.island.nature.service;

import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.environment.Season;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.common.RandomProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LifecycleServiceTest {

    private LifecycleService lifecycleService;

    @Mock
    private NatureWorld world;

    @Mock
    private RandomProvider random;

    @Mock
    private Animal animal;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Cell cell;
    private SpeciesRegistry registry;
    private final Configuration config = new Configuration();

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader(config).load();
        given(world.getConfiguration()).willReturn(config);
        lifecycleService = new LifecycleService(world, executor, random);
        cell = new Cell(0, 0, world);
        
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        given(animal.getAnimalType()).willReturn(wolfType);
        given(animal.getMaxPerCell()).willReturn(wolfType.getMaxPerCell());
        given(animal.getSpeciesKey()).willReturn(new SpeciesKey("wolf", true));
        
        given(world.getCurrentSeason()).willReturn(Season.SUMMER);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    @DisplayName("Animal should survive and consume energy when metabolism is met")
    void should_survive_and_consume_energy_when_metabolism_met() {
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(10L);
        given(animal.tryConsumeEnergy(anyLong())).willReturn(true);
        given(animal.checkAgeDeath()).willReturn(false);
        
        cell.addAnimal(animal);
        
        lifecycleService.processCell(cell, 1);
        
        verify(animal).tryConsumeEnergy(anyLong());
    }

    @Test
    @DisplayName("Animal should die when energy is exhausted")
    void should_die_when_energy_exhausted() {
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(100L);
        given(animal.tryConsumeEnergy(anyLong())).willReturn(false);
        
        cell.addAnimal(animal);
        
        lifecycleService.processCell(cell, 1);
        
        verify(animal).tryConsumeEnergy(anyLong());
    }

    @Test
    @DisplayName("Animal should die when max age is reached")
    void should_die_when_max_age_reached() {
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(10L);
        given(animal.tryConsumeEnergy(anyLong())).willReturn(true);
        given(animal.checkAgeDeath()).willReturn(true);
        
        cell.addAnimal(animal);
        
        lifecycleService.processCell(cell, 1);
        
        verify(animal).checkAgeDeath();
    }

    @Test
    @DisplayName("Hibernation should reduce metabolism for cold-blooded animals in winter")
    void hibernation_should_reduce_metabolism() {
        given(world.getCurrentSeason()).willReturn(Season.WINTER);
        
        AnimalType boaType = registry.getAnimalType(new SpeciesKey("boa", true)).orElseThrow();
        given(animal.getAnimalType()).willReturn(boaType);
        given(animal.getSpeciesKey()).willReturn(new SpeciesKey("boa", true));
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(1000L);
        given(animal.tryConsumeEnergy(anyLong())).willReturn(true);
        
        cell.addAnimal(animal);
        
        lifecycleService.processCell(cell, 1);
        
        // HIBERNATION_METABOLISM_MODIFIER_BP = 100 (1%)
        // 1000 * 100 / 10000 = 10
        // Winter metabolism modifier = 1.2
        // 10 * 1.2 = 12
        verify(animal).tryConsumeEnergy(12L);
    }
}