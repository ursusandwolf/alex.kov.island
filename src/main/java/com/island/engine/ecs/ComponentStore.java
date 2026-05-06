package com.island.engine.ecs;

import java.util.HashMap;
import java.util.Map;

public class ComponentStore {
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public <C extends Component> void add(C component) {
        components.put(component.getClass(), component);
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> C get(Class<C> type) {
        return (C) components.get(type);
    }

    public <C extends Component> boolean has(Class<C> type) {
        return components.containsKey(type);
    }

    public <C extends Component> void remove(Class<C> type) {
        components.remove(type);
    }
}