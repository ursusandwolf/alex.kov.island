package com.island.engine;

/**
 * Listener for world events, parametrized by the entity type T.
 */
public interface WorldListener<T> {
    void onEntityAdded(T entity);

    void onEntityRemoved(T entity);
}