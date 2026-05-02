package com.island.nature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.Season;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.nature.model.Cell;
import com.island.util.RandomProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
class LifecycleServiceTest {

    private LifecycleService lifecycleService;

    @Mock
    private NatureWorld world;

    @Mock
    private RandomProvider random;

    @Mock
    private Animal animal;

    private Cell cell;
    private SpeciesRegistry registry;
    private final Configuration config = new Configuration();

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader(config).load();
        given(world.getConfiguration()).willReturn(config);
        lifecycleService = new LifecycleService(world, Executors.newSingleThreadExecutor(), random);
        cell = new Cell(0, 0, world);
        
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        given(animal.getAnimalType()).willReturn(wolfType);
        given(animal.getMaxPerCell()).willReturn(wolfType.getMaxPerCell());
        given(animal.getSpeciesKey()).willReturn(SpeciesKey.WOLF);
        
        given(world.getCurrentSeason()).willReturn(Season.SUMMER);
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
        
        verify(animal).tryConsumeEnergy(10L);
        verify(world, never()).reportDeath(any(), any());
    }

    @Test
    @DisplayName("Animal should die when energy is exhausted")
    void should_die_when_energy_exhausted() {
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(100L);
        given(animal.tryConsumeEnergy(anyLong())).willReturn(false);
        
        cell.addAnimal(animal);
        
        lifecycleService.processCell(cell, 1);
        
        verify(world).reportDeath(SpeciesKey.WOLF, DeathCause.HUNGER);
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
        
        verify(world).reportDeath(SpeciesKey.WOLF, DeathCause.AGE);
    }

    @Test
    @DisplayName("Hibernation should reduce metabolism for cold-blooded animals in winter")
    void hibernation_should_reduce_metabolism() {
        given(world.getCurrentSeason()).willReturn(Season.WINTER);
        
        AnimalType boaType = registry.getAnimalType(SpeciesKey.BOA).orElseThrow();
        given(animal.getAnimalType()).willReturn(boaType);
        given(animal.getSpeciesKey()).willReturn(SpeciesKey.BOA);
        given(animal.isAlive()).willReturn(true);
        given(animal.getDynamicMetabolismRate()).willReturn(1000L);
        given(animal.tryConsumeEnergy(anyLong())).willReturn(true);
        
        cell.addAnimal(animal);
        
        lifecycleService.processCell(cell, 1);
        
        // HIBERNATION_METABOLISM_MODIFIER_BP = 100 (1%)
        // 1000 * 100 / 10000 = 10
        // Winter metabolism modifier = 1.2
        // 10 * 1.2 = 12
        verify(animal).tryConsumeEnergy(150L);
    }
}
