package com.island.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.island.config.SimulationConstants.SCALE_1M;

class OrganismTest {

    private static class TestOrganism extends Organism {
        protected TestOrganism(long maxEnergy, int maxLifespan) {
            super(maxEnergy, maxLifespan);
        }

        @Override
        public String getTypeName() {
            return "TestOrganism";
        }

        @Override
        public SpeciesKey getSpeciesKey() {
            return SpeciesKey.WOLF; // Dummy
        }
    }

    @Test
    @DisplayName("Energy should be scaled by 1,000_000 and handle precision")
    void testEnergyScaling() {
        long maxEnergy = 50 * SCALE_1M;
        Organism organism = new TestOrganism(maxEnergy, 10);
        
        // Initial energy is 50% of max by default (BIRTH_INITIAL factor is 0.5)
        assertEquals(maxEnergy / 2, organism.getCurrentEnergy());
        
        long val = (long) (10.555555 * SCALE_1M);
        organism.setEnergy(val);
        assertEquals(val, organism.getCurrentEnergy());
    }

    @Test
    @DisplayName("Energy consumption should be thread-safe and precise")
    void testEnergyConsumption() {
        Organism organism = new TestOrganism(100 * SCALE_1M, 10);
        organism.setEnergy(10 * SCALE_1M);
        
        boolean stillAlive = organism.tryConsumeEnergy((long) (9.999999 * SCALE_1M));
        assertTrue(stillAlive);
        assertEquals(1, organism.getCurrentEnergy()); // 1 micro-unit left
        
        stillAlive = organism.tryConsumeEnergy(1L);
        assertFalse(stillAlive, "Organism should die when energy reaches 0");
        assertFalse(organism.isAlive());
    }

    @Test
    @DisplayName("Energy should not exceed maxEnergy")
    void testMaxEnergyLimit() {
        Organism organism = new TestOrganism(100 * SCALE_1M, 10);
        organism.addEnergy(50 * SCALE_1M); 
        assertEquals(100 * SCALE_1M, organism.getCurrentEnergy());
    }
}
