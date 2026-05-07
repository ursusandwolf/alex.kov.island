package com.island.engine.ecs;

import com.island.engine.model.Mortal;

/**
 * Base interface for an entity in the Entity-Component-System architecture.
 */
public interface Entity extends Mortal {
    ComponentStore getComponentStore();

    default EntityArchetype getArchetype() {
        return null;
    }
}