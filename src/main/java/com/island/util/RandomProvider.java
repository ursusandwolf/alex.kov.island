package com.island.util;

/**
 * Interface for random number generation to support determinism in tests.
 */
public interface RandomProvider {
    int nextInt(int bound);

    int nextInt(int origin, int bound);

    long nextLong();

    double nextDouble();

    double nextDouble(double bound);
    
    default boolean checkChance(int chance) {
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return nextInt(100) < chance;
    }
}
