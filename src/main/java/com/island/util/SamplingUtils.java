package com.island.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Utility for performing efficient sampling of collections during simulation ticks.
 */
public class SamplingUtils {

    /**
     * Iterates over a sample of the collection using stride-based sampling with a random start offset.
     * This avoids full iteration of large collections when a limit is reached.
     *
     * @param collection The collection to sample.
     * @param limit      The maximum number of items to process.
     * @param random     The random provider for the start offset.
     * @param action     The action to perform on each sampled item.
     * @param <T>        The type of elements.
     */
    public static <T> void forEachSampled(Collection<T> collection, int limit, RandomProvider random, Consumer<T> action) {
        int size = collection.size();
        if (size == 0 || limit <= 0) {
            return;
        }

        int step = (size > limit) ? (size / limit + 1) : 1;
        int startOffset = (size > limit) ? random.nextInt(step) : 0;

        Iterator<T> it = collection.iterator();
        int currentIndex = 0;
        int processedCount = 0;

        // Skip to start offset
        while (it.hasNext() && currentIndex < startOffset) {
            it.next();
            currentIndex++;
        }

        // Sample with stride
        while (it.hasNext() && processedCount < limit) {
            T item = it.next();
            if ((currentIndex - startOffset) % step == 0) {
                action.accept(item);
                processedCount++;
            }
            currentIndex++;
        }
    }

    /**
     * Alternative sampling for Lists where indexed access might be faster, 
     * but stride-based iteration is used here for consistency and safety with all collection types.
     */
    public static <T> void forEachSampled(java.util.List<T> list, int limit, RandomProvider random, Consumer<T> action) {
        forEachSampled((Collection<T>) list, limit, random, action);
    }
}
