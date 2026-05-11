package com.island.util.sampling;

import com.island.util.common.RandomProvider;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplingUtilsTest {

    @Test
    void testForEachSampledWithRandomAccessList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger count = new AtomicInteger();
        RandomProvider random = new DummyRandomProvider(0);

        SamplingUtils.forEachSampled(list, 5, random, item -> count.incrementAndGet());

        assertTrue(count.get() <= 5 && count.get() > 0, "Should process up to the limit items (stride-based)");
    }

    @Test
    void testForEachSampledWithSetIterator() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        AtomicInteger count = new AtomicInteger();
        RandomProvider random = new DummyRandomProvider(0);

        SamplingUtils.forEachSampled(set, 5, random, item -> count.incrementAndGet());

        assertTrue(count.get() <= 5 && count.get() > 0, "Should process up to the limit items for Set (stride-based)");
    }

    @Test
    void testForEachSampledSmallCollection() {
        List<Integer> list = Arrays.asList(1, 2);
        AtomicInteger count = new AtomicInteger();
        RandomProvider random = new DummyRandomProvider(0);

        SamplingUtils.forEachSampled(list, 5, random, item -> count.incrementAndGet());

        assertEquals(2, count.get(), "Should process all items if limit > size");
    }

    @Test
    void testForEachSampledEmpty() {
        List<Integer> list = List.of();
        AtomicInteger count = new AtomicInteger();
        RandomProvider random = new DummyRandomProvider(0);

        SamplingUtils.forEachSampled(list, 5, random, item -> count.incrementAndGet());

        assertEquals(0, count.get(), "Should process 0 items for empty list");
    }

    static class DummyRandomProvider implements RandomProvider {
        private final int valueToReturn;
        DummyRandomProvider(int valueToReturn) { this.valueToReturn = valueToReturn; }
        @Override public int nextInt(int bound) { return valueToReturn; }
        @Override public int nextInt(int origin, int bound) { return valueToReturn; }
        @Override public long nextLong() { return valueToReturn; }
        @Override public double nextDouble() { return valueToReturn; }
        @Override public double nextDouble(double bound) { return valueToReturn; }
        @Override public boolean nextBoolean() { return false; }
    }
}
