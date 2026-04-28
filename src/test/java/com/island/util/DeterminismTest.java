package com.island.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeterminismTest {

    @Test
    void testDeterministicRandom() {
        // Mock provider that always returns fixed values
        RandomProvider mockProvider = new RandomProvider() {
            @Override
            public int nextInt(int bound) { return 42 % bound; }
            @Override
            public int nextInt(int origin, int bound) { return origin; }
            @Override
            public double nextDouble() { return 0.5; }
            @Override
            public double nextDouble(double bound) { return bound / 2.0; }
        };

        RandomUtils.setProvider(mockProvider);

        assertEquals(42 % 100, RandomUtils.nextInt(100));
        assertEquals(0.5, RandomUtils.nextDouble());
        assertTrue(RandomUtils.checkChance(60), "0.5 < 0.6 should be true");
        assertFalse(RandomUtils.checkChance(40), "0.5 < 0.4 should be false");

        // Restore default provider
        RandomUtils.setProvider(new DefaultRandomProvider());
    }
}
