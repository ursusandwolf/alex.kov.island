package com.island.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for random number generation.
 */
public final class RandomUtils {
    
    private RandomUtils() {
    }

    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }

    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static double nextDouble(double bound) {
        return ThreadLocalRandom.current().nextDouble(bound);
    }

    public static boolean checkChance(int chance) {
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return nextInt(100) < chance;
    }
}
