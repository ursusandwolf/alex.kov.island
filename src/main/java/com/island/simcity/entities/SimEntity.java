package com.island.simcity.entities;

import com.island.engine.ecs.ArrayComponentStore;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.ecs.ComponentStore;
import com.island.engine.ecs.Entity;
import com.island.engine.ecs.EntityArchetype;
import lombok.Getter;

public class SimEntity implements Entity {
    private final ComponentStore componentStore;
    private final ComponentRegistry componentRegistry;
    private volatile EntityArchetype archetype;
    private boolean alive = true;

    public SimEntity(ComponentRegistry registry) {
        this.componentRegistry = registry;
        this.componentStore = new ArrayComponentStore(registry);
    }

    public <C extends Component> void addComponent(C component) {
        componentStore.add(component);
        updateArchetype();
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> C getComponent(Class<C> type) {
        return componentStore.get(type);
    }

    public <C extends Component> boolean hasComponent(Class<C> type) {
        return componentStore.has(type);
    }

    private void updateArchetype() {
        this.archetype = componentRegistry.getArchetype(componentStore.getComponentBitSet());
    }

    @Override
    public EntityArchetype getArchetype() {
        return archetype;
    }

    @Override
    public ComponentStore getComponentStore() {
        return componentStore;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void die() {
        this.alive = false;
    }

    @Override
    public String getTypeName() {
        return "SimEntity";
    }
}