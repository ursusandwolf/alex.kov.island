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

    public void addComponent(Component component) {
        componentStore.add(component);
        updateArchetype();
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