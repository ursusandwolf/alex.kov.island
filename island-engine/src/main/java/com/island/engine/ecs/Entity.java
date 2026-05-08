package com.island.engine.ecs;

import com.island.engine.model.Mortal;

/**
 * Base interface for an entity in the Entity-Component-System architecture.
 */
public interface Entity extends Mortal {
    ComponentStore getComponentStore();

    default <C extends Component> C getComponent(Class<C> type) {
        return getComponentStore().get(type);
    }

    default <C extends Component> boolean hasComponent(Class<C> type) {
        return getComponentStore().has(type);
    }

    default EntityArchetype getArchetype() {
        return null;
    }
}