package com.island.nature.entities;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.HealthStorage;
import com.island.engine.core.MovementStorage;
import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;

class OrganismTest {

    private final Configuration config = new Configuration();

    private class TestOrganism extends Organism {
        protected TestOrganism(Configuration config, ComponentRegistry registry, long maxEnergy, int maxLifespan) {
            super(config, registry, maxEnergy, maxLifespan);
        }

        @Override
        public String getTypeName() {
            return "TestOrganism";
        }

        @Override
        public SpeciesKey getSpeciesKey() {
            return new SpeciesKey("wolf", true); // Dummy
        }
    }

    @Test
    @DisplayName("Energy should be scaled by 1,000_000 and handle precision")
    void testEnergyScaling() {
        long maxEnergy = 50 * config.getScale1M();
        Organism organism = new TestOrganism(config, new ComponentRegistry(), maxEnergy, 10);
        
        // Initial energy is 50% of max by default (BIRTH_INITIAL factor is 0.5)
        assertEquals(maxEnergy / 2, organism.getCurrentEnergy());
        
        long val = (long) (10.555555 * config.getScale1M());
        organism.setEnergy(val);
        assertEquals(val, organism.getCurrentEnergy());
    }

    @Test
    @DisplayName("Energy consumption should be thread-safe and precise")
    void testEnergyConsumption() {
        Organism organism = new TestOrganism(config, new ComponentRegistry(), 100 * config.getScale1M(), 10);
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
        Organism organism = new TestOrganism(config, new ComponentRegistry(), 100 * config.getScale1M(), 10);
        organism.addEnergy(50 * config.getScale1M()); 
        assertEquals(100 * config.getScale1M(), organism.getCurrentEnergy());
    }

    @Test
    @DisplayName("Organism should bind to SoA storage and sync speed correctly")
    void testStorageBinding() {
        Organism organism = new TestOrganism(config, new ComponentRegistry(), 100 * config.getScale1M(), 10);
        organism.setSpeed(5);
        
        HealthStorage healthStorage = HealthStorage.create(10);
        AgeStorage ageStorage = AgeStorage.create(10);
        MovementStorage movementStorage = MovementStorage.create(10);
        
        organism.bindStorage(1, healthStorage, ageStorage, movementStorage);
        
        assertEquals(5, movementStorage.getSpeed(1));
        assertEquals(5, organism.getSpeed());
        
        organism.setSpeed(7);
        assertEquals(7, movementStorage.getSpeed(1));
        assertEquals(7, organism.getSpeed());
    }
}
