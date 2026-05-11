package com.island.util.common;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default implementation using ThreadLocalRandom for high performance,
 * or java.util.Random if a seed is provided for determinism.
 */
public class DefaultRandomProvider implements RandomProvider {
    private final Random seededRandom;

    public DefaultRandomProvider() {
        this.seededRandom = null;
    }

    public DefaultRandomProvider(long seed) {
        this.seededRandom = new Random(seed);
    }

    @Override
    public int nextInt(int bound) {
        return seededRandom != null ? seededRandom.nextInt(bound) : ThreadLocalRandom.current().nextInt(bound);
    }

    @Override
    public int nextInt(int origin, int bound) {
        return seededRandom != null ? seededRandom.nextInt(origin, bound) : ThreadLocalRandom.current().nextInt(origin, bound);
    }

    @Override
    public long nextLong() {
        return seededRandom != null ? seededRandom.nextLong() : ThreadLocalRandom.current().nextLong();
    }

    @Override
    public double nextDouble() {
        return seededRandom != null ? seededRandom.nextDouble() : ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public double nextDouble(double bound) {
        return seededRandom != null ? seededRandom.nextDouble() * bound : ThreadLocalRandom.current().nextDouble(bound);
    }

    @Override
    public boolean nextBoolean() {
        return seededRandom != null ? seededRandom.nextBoolean() : ThreadLocalRandom.current().nextBoolean();
    }
}
