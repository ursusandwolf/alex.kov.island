package com.island.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InteractionMatrixTest {
    private InteractionMatrix matrix;
    private SpeciesRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader().load();
        matrix = new InteractionMatrix(registry);
    }

    @Test
    void testSetAndGetChance() {
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 60);
        assertEquals(60, matrix.getChance(SpeciesKey.WOLF, SpeciesKey.RABBIT));
    }

    @Test
    void testGetDefaultZero() {
        assertEquals(0, matrix.getChance(SpeciesKey.BEAR, SpeciesKey.MOUSE));
    }

    @Test
    void testFreezeAndModify() {
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 60);
        matrix.freeze();
        
        // Modification after freeze should trigger copy and work correctly
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 80);
        assertEquals(80, matrix.getChance(SpeciesKey.WOLF, SpeciesKey.RABBIT));
    }
}
