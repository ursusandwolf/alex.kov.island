package com.island.util;

/**
 * Utility class for random number generation.
 * Now delegates to a RandomProvider to allow deterministic tests.
 */
public final class RandomUtils {
    private static RandomProvider provider = new DefaultRandomProvider();
    
    private RandomUtils() {
    }

    public static void setProvider(RandomProvider newProvider) {
        provider = newProvider;
    }

    public static int nextInt(int bound) {
        return provider.nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        return provider.nextInt(origin, bound);
    }

    public static double nextDouble() {
        return provider.nextDouble();
    }

    public static double nextDouble(double bound) {
        return provider.nextDouble(bound);
    }

    public static boolean checkChance(int chance) {
        return provider.checkChance(chance);
    }
}
