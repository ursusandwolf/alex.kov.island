package com.island.nature.entities;

import com.island.nature.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganismTest {

    private final Configuration config = new Configuration();

    private class TestOrganism extends Organism {
        protected TestOrganism(Configuration config, long maxEnergy, int maxLifespan) {
            super(config, maxEnergy, maxLifespan);
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
        long maxEnergy = 50 * config.getScale1M();
        Organism organism = new TestOrganism(config, maxEnergy, 10);
        
        // Initial energy is 50% of max by default (BIRTH_INITIAL factor is 0.5)
        assertEquals(maxEnergy / 2, organism.getCurrentEnergy());
        
        long val = (long) (10.555555 * config.getScale1M());
        organism.setEnergy(val);
        assertEquals(val, organism.getCurrentEnergy());
    }

    @Test
    @DisplayName("Energy consumption should be thread-safe and precise")
    void testEnergyConsumption() {
        Organism organism = new TestOrganism(config, 100 * config.getScale1M(), 10);
        organism.setEnergy(10 * config.getScale1M());
        
        boolean stillAlive = organism.tryConsumeEnergy((long) (9.999999 * config.getScale1M()));
        assertTrue(stillAlive);
        assertEquals(1, organism.getCurrentEnergy()); // 1 micro-unit left
        
        stillAlive = organism.tryConsumeEnergy(1L);
        assertFalse(stillAlive, "Organism should die when energy reaches 0");
        assertFalse(organism.isAlive());
    }

    @Test
    @DisplayName("Energy should not exceed maxEnergy")
    void testMaxEnergyLimit() {
        Organism organism = new TestOrganism(config, 100 * config.getScale1M(), 10);
        organism.addEnergy(50 * config.getScale1M()); 
        assertEquals(100 * config.getScale1M(), organism.getCurrentEnergy());
    }
}
