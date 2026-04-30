package com.island.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganismTest {

    private static class TestOrganism extends Organism {
        protected TestOrganism(double maxEnergy, int maxLifespan) {
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
        double maxEnergy = 50.0;
        Organism organism = new TestOrganism(maxEnergy, 10);
        
        // Initial energy is 50% of max by default (BIRTH_INITIAL factor is 0.5)
        // Let's check getCurrentEnergy() returns correct double
        assertEquals(maxEnergy * 0.5, organism.getCurrentEnergy(), 0.000001);
        
        organism.setEnergy(10.555555);
        assertEquals(10.555555, organism.getCurrentEnergy(), 0.000001);
    }

    @Test
    @DisplayName("Energy consumption should be thread-safe and precise")
    void testEnergyConsumption() {
        Organism organism = new TestOrganism(100.0, 10);
        organism.setEnergy(10.0);
        
        boolean stillAlive = organism.tryConsumeEnergy(9.999999);
        assertTrue(stillAlive);
        assertEquals(0.000001, organism.getCurrentEnergy(), 0.0000001);
        
        stillAlive = organism.tryConsumeEnergy(0.000001);
        assertFalse(stillAlive, "Organism should die when energy < 1 micro-unit");
        assertFalse(organism.isAlive());
    }

    @Test
    @DisplayName("Energy should not exceed maxEnergy")
    void testMaxEnergyLimit() {
        Organism organism = new TestOrganism(100.0, 10);
        organism.addEnergy(50.0); // 70 + 50 = 120, but max is 100
        assertEquals(100.0, organism.getCurrentEnergy());
    }
}
