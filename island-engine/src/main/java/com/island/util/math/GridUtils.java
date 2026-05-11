package com.island.util.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.Mortal;

/**
 * Shared utility for grid-based operations across different simulation worlds.
 */
public class GridUtils {

    public static boolean isValid(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public static <T extends Mortal> List<SimulationNode<T>> getNeighbors(SimulationWorld<T> world, SimulationNode<T> node, int width, int height) {
        List<SimulationNode<T>> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                world.getNode(node, dx, dy).ifPresent(neighbors::add);
            }
        }
        return neighbors;
    }

    public static void executeWithDoubleLock(SimulationNode<?> n1, SimulationNode<?> n2, Runnable action) {
        if (n1 == n2) {
            n1.getLock().lock();
            try {
                action.run();
            } finally {
                n1.getLock().unlock();
            }
            return;
        }

        int hash1 = System.identityHashCode(n1);
        int hash2 = System.identityHashCode(n2);

        if (hash1 == hash2) {
            // Handle rare identityHashCode collision with tryLock backoff to prevent deadlock
            Lock lock1 = n1.getLock();
            Lock lock2 = n2.getLock();
            while (true) {
                lock1.lock();
                if (lock2.tryLock()) {
                    try {
                        action.run();
                        return;
                    } finally {
                        lock2.unlock();
                        lock1.unlock();
                    }
                } else {
                    lock1.unlock();
                    Thread.yield();
                }
            }
        }

        SimulationNode<?> first = (hash1 < hash2) ? n1 : n2;
        SimulationNode<?> second = (first == n1) ? n2 : n1;

        Lock firstLock = first.getLock();
        Lock secondLock = second.getLock();

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                action.run();
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }
}