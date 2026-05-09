package com.island.engine.internal;

import com.island.engine.core.InternalEngine;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.ecs.ComponentStore;
import java.util.Arrays;
import java.util.BitSet;

/**
 * High-performance implementation of ComponentStore using an array.
 * Best for entities with a fixed set of components.
 * Automatically grows the internal array if needed.
 */
@InternalEngine
public class ArrayComponentStore implements ComponentStore {
    private static final Component[] EMPTY_COMPONENTS = new Component[0];
    private final ComponentRegistry registry;
    private Component[] components;

    public ArrayComponentStore(ComponentRegistry registry) {
        this.registry = registry;
        this.components = EMPTY_COMPONENTS;
    }

    @Override
    public <C extends Component> void add(C component) {
        int index = registry.getOrRegister(component.getClass());
        ensureCapacity(index);
        components[index] = component;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Component> C get(Class<C> type) {
        int index = registry.getOrRegister(type);
        if (index < components.length) {
            return (C) components[index];
        }
        return null;
    }

    @Override
    public <C extends Component> boolean has(Class<C> type) {
        int index = registry.getOrRegister(type);
        return index < components.length && components[index] != null;
    }

    @Override
    public <C extends Component> void remove(Class<C> type) {
        int index = registry.getOrRegister(type);
        if (index < components.length) {
            components[index] = null;
        }
    }

    @Override
    public BitSet getComponentBitSet() {
        BitSet bitSet = new BitSet();
        for (int i = 0; i < components.length; i++) {
            if (components[i] != null) {
                bitSet.set(i);
            }
        }
        return bitSet;
    }

    private void ensureCapacity(int index) {
        if (index >= components.length) {
            components = Arrays.copyOf(components, Math.max(index + 1, components.length * 2));
        }
    }
}
