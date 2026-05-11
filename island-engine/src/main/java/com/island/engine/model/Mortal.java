package com.island.engine.model;

import com.island.engine.core.EngineAPI;

/**
 * Represents any entity that can live and die in the simulation.
 */
@EngineAPI
public interface Mortal {
    /**
     * Checks if the entity is still alive.
     */
    boolean isAlive();

    /**
     * Marks the entity as dead.
     */
    void die();

    /**
     * Gets the type identifier of the entity.
     */
    String getTypeName();
}