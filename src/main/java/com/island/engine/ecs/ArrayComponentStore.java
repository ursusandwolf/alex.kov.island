package com.island.engine.ecs;

import java.util.Arrays;

/**
 * High-performance implementation of ComponentStore using an array.
 * Best for entities with a fixed set of components.
 * Automatically grows the internal array if needed.
 */
public class ArrayComponentStore implements ComponentStore {
    private final ComponentRegistry registry;
    private Component[] components;

    public ArrayComponentStore(ComponentRegistry registry) {
        this.registry = registry;
        // Initial size based on current registry or a small default
        this.components = new Component[Math.max(registry.size(), 8)];
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

    private void ensureCapacity(int index) {
        if (index >= components.length) {
            components = Arrays.copyOf(components, Math.max(index + 1, components.length * 2));
        }
    }
}
