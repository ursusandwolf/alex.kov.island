package com.island.content;

import com.island.config.EnergyPolicy;
import com.island.content.animals.predators.Wolf;
import com.island.content.animals.herbivores.Rabbit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.island.config.SimulationConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyConsumptionTest {
    private final SpeciesRegistry registry = new SpeciesLoader().load();

    @Test
    @DisplayName("Test energy consumption during movement based on speed")
    void testMovementEnergyCost() {
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        double initialEnergy = wolf.getCurrentEnergy();
        double expectedCost = wolf.getMaxEnergy() * (BASE_MOVE_COST_PERCENT + (wolf.getSpeed() * SPEED_MOVE_COST_STEP_PERCENT));

        // Simulating the energy cost logic now handled by MovementService
        if (wolf.canPerformAction()) {
            wolf.consumeEnergy(expectedCost);
        }

        assertEquals(initialEnergy - expectedCost, wolf.getCurrentEnergy(), 0.001);
    }

    @Test
    @DisplayName("Test reproduction energy cost")
    void testReproductionEnergyCost() {
        Rabbit rabbit = new Rabbit(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        rabbit.addEnergy(rabbit.getMaxEnergy());
        double initialEnergy = rabbit.getCurrentEnergy();
        double cost = rabbit.getMaxEnergy() * EnergyPolicy.REPRODUCTION_COST.getFactor();

        // Simulating the energy cost logic now handled by ReproductionService
        if (rabbit.canInitiateReproduction()) {
            if (rabbit.getCurrentEnergy() > cost) {
                rabbit.consumeEnergy(cost);
            }
        }

        assertEquals(initialEnergy - cost, rabbit.getCurrentEnergy(), 0.001);
    }
    @Test
    @DisplayName("Test organism dies when energy reaches zero")
    void testDeathByEnergyExhaustion() {
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        assertTrue(wolf.isAlive());
        
        wolf.consumeEnergy(wolf.getMaxEnergy());
        
        assertFalse(wolf.isAlive());
        assertEquals(0, wolf.getCurrentEnergy());
    }

    @Test
    @DisplayName("Test metabolism reduces energy")
    void testMetabolism() {
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        double initialEnergy = wolf.getCurrentEnergy();
        double metabolismCost = wolf.getMaxEnergy() * BASE_METABOLISM_PERCENT;
        
        wolf.consumeEnergy(metabolismCost);
        
        assertEquals(initialEnergy - metabolismCost, wolf.getCurrentEnergy(), 0.001);
    }
}
