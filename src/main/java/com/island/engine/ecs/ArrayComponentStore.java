package com.island.engine.ecs;

/**
 * High-performance implementation of ComponentStore using an array.
 * Best for entities with a fixed set of components.
 */
public class ArrayComponentStore implements ComponentStore {
    private final Component[] components;

    public ArrayComponentStore() {
        // Initial size based on current registry, but it might grow.
        // For a real game engine, we'd pre-register all components.
        this.components = new Component[32]; // Sufficient for this simulation
    }

    @Override
    public <C extends Component> void add(C component) {
        int index = ComponentRegistry.getIndex(component.getClass());
        if (index < components.length) {
            components[index] = component;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Component> C get(Class<C> type) {
        int index = ComponentRegistry.getIndex(type);
        if (index < components.length) {
            return (C) components[index];
        }
        return null;
    }

    @Override
    public <C extends Component> boolean has(Class<C> type) {
        int index = ComponentRegistry.getIndex(type);
        return index < components.length && components[index] != null;
    }

    @Override
    public <C extends Component> void remove(Class<C> type) {
        int index = ComponentRegistry.getIndex(type);
        if (index < components.length) {
            components[index] = null;
        }
    }
}
