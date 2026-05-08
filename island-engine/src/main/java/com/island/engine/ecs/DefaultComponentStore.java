package com.island.engine.ecs;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ComponentStore using a ConcurrentHashMap.
 */
public class DefaultComponentStore implements ComponentStore {
    private final ComponentRegistry registry;
    private final Map<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();

    public DefaultComponentStore(ComponentRegistry registry) {
        this.registry = registry;
    }

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

    @Override
    public BitSet getComponentBitSet() {
        BitSet bitSet = new BitSet();
        for (Class<? extends Component> type : components.keySet()) {
            bitSet.set(registry.getOrRegister(type));
        }
        return bitSet;
    }
}
