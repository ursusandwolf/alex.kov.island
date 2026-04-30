package com.island.engine;

/**
 * Represents any entity that can live and die in the simulation.
 */
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
