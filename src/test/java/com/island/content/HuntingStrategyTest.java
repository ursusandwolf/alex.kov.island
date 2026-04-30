package com.island.content;

import com.island.util.InteractionMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.island.config.SimulationConstants.SCALE_1M;

class HuntingStrategyTest {
    private InteractionMatrix matrix;
    private DefaultHuntingStrategy strategy;
    private Animal wolf;
    private Animal rabbit;

    @BeforeEach
    void setUp() {
        matrix = mock(InteractionMatrix.class);
        strategy = new DefaultHuntingStrategy(matrix);
        
        wolf = mock(Animal.class);
        when(wolf.getSpeciesKey()).thenReturn(SpeciesKey.WOLF);
        when(wolf.getMaxEnergy()).thenReturn(100L * SCALE_1M);
        when(wolf.getSpeed()).thenReturn(3);

        rabbit = mock(Animal.class);
        when(rabbit.getSpeciesKey()).thenReturn(SpeciesKey.RABBIT);
        when(rabbit.getWeight()).thenReturn(2L * SCALE_1M);
        when(rabbit.getSpeed()).thenReturn(2);
    }

    @Test
    void calculateSuccessRate_returnsCorrectValue() {
        when(matrix.getChance(SpeciesKey.WOLF, SpeciesKey.RABBIT)).thenReturn(60);
        int rateBP = strategy.calculateSuccessRate(wolf, rabbit);
        assertEquals(6000, rateBP);
    }

    @Test
    void calculateHuntCost_chaseCostAppliedWhenPreyIsFaster() {
        when(rabbit.getSpeed()).thenReturn(5); // Faster than wolf (3)
        // strikeCost = min(2 * 0.1, 100 * 0.005) = min(0.2, 0.5) = 0.2
        // speedDiff = 5 - 3 = 2
        // chaseCost = 100 * (2 * 0.05) = 10.0
        // total = 10.2
        
        long cost = strategy.calculateHuntCost(wolf, rabbit);
        assertEquals((long) (10.2 * SCALE_1M), cost);
    }

    @Test
    void calculateHuntCost_noChaseCostWhenPreyIsSlower() {
        when(rabbit.getSpeed()).thenReturn(1); // Slower than wolf (3)
        long cost = strategy.calculateHuntCost(wolf, rabbit);
        assertEquals((long) (0.2 * SCALE_1M), cost);
    }

    @Test
    void isWorthHunting_returnsTrueWhenROIHigh() {
        // Gain = 2.0 * 0.8 = 1.6
        // Cost = 0.2
        // ROI threshold = 1.1. Gain (1.6) > Cost (0.2) * 1.1 (0.22) -> True
        assertTrue(strategy.isWorthHunting(wolf, rabbit, 8000, (long) (0.2 * SCALE_1M)));
    }

    @Test
    void isWorthHunting_returnsFalseWhenROILow() {
        // Gain = 2.0 * 0.1 = 0.2
        // Cost = 0.2
        // ROI threshold = 1.1. Gain (0.2) < Cost (0.2) * 1.1 (0.22) -> False
        assertFalse(strategy.isWorthHunting(wolf, rabbit, 1000, (long) (0.2 * SCALE_1M)));
    }
}
