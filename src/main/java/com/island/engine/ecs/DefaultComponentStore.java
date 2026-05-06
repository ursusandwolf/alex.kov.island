package com.island.engine.ecs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ComponentStore using a ConcurrentHashMap.
 */
public class DefaultComponentStore implements ComponentStore {
    private final Map<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();

    @Override
    public <C extends Component> void add(C component) {
        components.put(component.getClass(), component);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Component> C get(Class<C> type) {
        return (C) components.get(type);
    }

    @Override
    public <C extends Component> boolean has(Class<C> type) {
        return components.containsKey(type);
    }

    @Override
    public <C extends Component> void remove(Class<C> type) {
        components.remove(type);
    }
}
