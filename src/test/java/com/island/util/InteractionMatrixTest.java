package com.island.util;

import com.island.content.SpeciesKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InteractionMatrixTest {
    private InteractionMatrix matrix;

    @BeforeEach
    void setUp() {
        matrix = new InteractionMatrix();
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
