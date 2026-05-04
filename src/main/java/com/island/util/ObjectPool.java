package com.island.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * A simple thread-safe object pool.
 */
public class ObjectPool<T extends Poolable> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;

    public ObjectPool(Supplier<T> factory) {
        this.factory = factory;
    }

    /**
     * Acquires an object from the pool or creates a new one.
     */
    public T acquire() {
        T obj = pool.poll();
        if (obj == null) {
            return factory.get();
        }
        return obj;
    }

    /**
     * Returns an object to the pool for later reuse.
     */
    public void release(T obj) {
        if (obj != null) {
            obj.reset();
            pool.offer(obj);
        }
    }

    public int size() {
        return pool.size();
    }
}
