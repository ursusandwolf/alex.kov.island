package com.island.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Default implementation using ThreadLocalRandom for high performance.
 */
public class DefaultRandomProvider implements RandomProvider {
    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    @Override
    public int nextInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }

    @Override
    public long nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public double nextDouble(double bound) {
        return ThreadLocalRandom.current().nextDouble(bound);
    }
}
