package com.island.util;

import com.island.engine.Mortal;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

/**
 * Shared utility for grid-based operations across different simulation worlds.
 */
public class GridUtils {

    public static boolean isValid(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public static <T extends Mortal> List<SimulationNode<T>> getNeighbors(SimulationWorld<T, ?> world, SimulationNode<T> node, int width, int height) {
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

    public static void executeWithDoubleLock(SimulationNode<?> n1, SimulationNode<?> n2, int x1, int y1, int x2, int y2, Runnable action) {
        if (n1 == n2) {
            n1.getLock().lock();
            try {
                action.run();
            } finally {
                n1.getLock().unlock();
            }
            return;
        }

        SimulationNode<?> first = (x1 < x2 || (x1 == x2 && y1 < y2)) ? n1 : n2;
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
