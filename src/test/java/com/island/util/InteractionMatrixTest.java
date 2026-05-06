package com.island.util;

import com.island.nature.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.interaction.InteractionMatrix;

class InteractionMatrixTest {
    private InteractionMatrix matrix;
    private SpeciesRegistry registry;

    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        registry = new SpeciesLoader(config).load();
        matrix = new InteractionMatrix(registry);
    }

    @Test
    void testSetAndGetChance() {
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("rabbit", false), 60);
        assertEquals(60, matrix.getChance(new SpeciesKey("wolf", true), new SpeciesKey("rabbit", false)));
    }

    @Test
    void testGetDefaultZero() {
        assertEquals(0, matrix.getChance(new SpeciesKey("bear", true), new SpeciesKey("mouse", false)));
    }

    @Test
    void testFreezeAndModify() {
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("rabbit", false), 60);
        matrix.freeze();
        
        // Modification after freeze should trigger copy and work correctly
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("rabbit", false), 80);
        assertEquals(80, matrix.getChance(new SpeciesKey("wolf", true), new SpeciesKey("rabbit", false)));
    }
}