package com.island.engine;

public interface WorldListener {
    void onEntityAdded(Object key);

    void onEntityRemoved(Object key);
}