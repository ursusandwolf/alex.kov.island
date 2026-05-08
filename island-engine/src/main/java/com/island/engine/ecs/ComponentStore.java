package com.island.engine.ecs;

import java.util.BitSet;

/**
 * Interface for typed component storage.
 */
public interface ComponentStore {
    <C extends Component> void add(C component);

    <C extends Component> C get(Class<C> type);

    <C extends Component> boolean has(Class<C> type);

    <C extends Component> void remove(Class<C> type);

    BitSet getComponentBitSet();
}
