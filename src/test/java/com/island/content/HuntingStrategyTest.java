package com.island.content;

import com.island.util.InteractionMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        when(wolf.getMaxEnergy()).thenReturn(100.0);
        when(wolf.getSpeed()).thenReturn(3);

        rabbit = mock(Animal.class);
        when(rabbit.getSpeciesKey()).thenReturn(SpeciesKey.RABBIT);
        when(rabbit.getWeight()).thenReturn(2.0);
        when(rabbit.getSpeed()).thenReturn(2);
    }

    @Test
    void calculateSuccessRate_returnsCorrectValue() {
        when(matrix.getChance(SpeciesKey.WOLF, SpeciesKey.RABBIT)).thenReturn(60);
        double rate = strategy.calculateSuccessRate(wolf, rabbit);
        assertEquals(0.6, rate, 0.001);
    }

    @Test
    void calculateHuntCost_chaseCostAppliedWhenPreyIsFaster() {
        when(rabbit.getSpeed()).thenReturn(5); // Faster than wolf (3)
        // strikeCost = min(2 * 0.1, 100 * 0.005) = min(0.2, 0.5) = 0.2
        // speedDiff = 5 - 3 = 2
        // chaseCost = 100 * (2 * 0.05) = 10.0
        // total = 10.2
        
        double cost = strategy.calculateHuntCost(wolf, rabbit);
        assertEquals(10.2, cost, 0.001);
    }

    @Test
    void calculateHuntCost_noChaseCostWhenPreyIsSlower() {
        when(rabbit.getSpeed()).thenReturn(1); // Slower than wolf (3)
        double cost = strategy.calculateHuntCost(wolf, rabbit);
        assertEquals(0.2, cost, 0.001);
    }

    @Test
    void isWorthHunting_returnsTrueWhenROIHigh() {
        // Gain = 2.0 * 0.8 = 1.6
        // Cost = 0.2
        // ROI threshold = 1.1. Gain (1.6) > Cost (0.2) * 1.1 (0.22) -> True
        assertTrue(strategy.isWorthHunting(wolf, rabbit, 0.8, 0.2));
    }

    @Test
    void isWorthHunting_returnsFalseWhenROILow() {
        // Gain = 2.0 * 0.1 = 0.2
        // Cost = 0.2
        // ROI threshold = 1.1. Gain (0.2) < Cost (0.2) * 1.1 (0.22) -> False
        assertFalse(strategy.isWorthHunting(wolf, rabbit, 0.1, 0.2));
    }
}
