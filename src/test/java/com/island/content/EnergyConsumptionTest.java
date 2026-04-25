package com.island.content;

import com.island.content.animals.predators.Wolf;
import com.island.content.animals.herbivores.Rabbit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.island.config.SimulationConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyConsumptionTest {
    private final SpeciesConfig config = SpeciesConfig.getInstance();

    @Test
    @DisplayName("Test energy consumption during movement based on speed")
    void testMovementEnergyCost() {
        Wolf wolf = new Wolf(config.getAnimalType("wolf")); // Speed 3
        double initialEnergy = wolf.getCurrentEnergy();
        double expectedCost = wolf.getMaxEnergy() * (BASE_MOVE_COST_PERCENT + (wolf.getSpeed() * SPEED_MOVE_COST_STEP_PERCENT));
        
        wolf.move();
        
        assertEquals(initialEnergy - expectedCost, wolf.getCurrentEnergy(), 0.001);
    }

    @Test
    @DisplayName("Test reproduction energy cost")
    void testReproductionEnergyCost() {
        Rabbit rabbit = new Rabbit(config.getAnimalType("rabbit"));
        rabbit.addEnergy(rabbit.getMaxEnergy()); // Ensure it's at max
        double initialEnergy = rabbit.getCurrentEnergy();
        double cost = rabbit.getMaxEnergy() * REPRODUCTION_COST_PERCENT;
        
        Animal baby = rabbit.reproduce();
        
        assertNotNull(baby, "Baby should be created when energy is sufficient.");
        assertEquals(initialEnergy - cost, rabbit.getCurrentEnergy(), 0.001);
    }

    @Test
    @DisplayName("Test organism dies when energy reaches zero")
    void testDeathByEnergyExhaustion() {
        Wolf wolf = new Wolf(config.getAnimalType("wolf"));
        assertTrue(wolf.isAlive());
        
        wolf.consumeEnergy(wolf.getMaxEnergy());
        
        assertFalse(wolf.isAlive());
        assertEquals(0, wolf.getCurrentEnergy());
    }

    @Test
    @DisplayName("Test metabolism reduces energy over time")
    void testMetabolism() {
        Wolf wolf = new Wolf(config.getAnimalType("wolf"));
        double initialEnergy = wolf.getCurrentEnergy();
        double metabolismCost = wolf.getMaxEnergy() * BASE_METABOLISM_PERCENT;
        
        wolf.checkState();
        
        assertEquals(initialEnergy - metabolismCost, wolf.getCurrentEnergy(), 0.001);
    }
}
