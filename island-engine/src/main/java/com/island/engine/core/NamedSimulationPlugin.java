package com.island.engine.core;

import com.island.engine.model.Mortal;

/**
 * Extension of {@link SimulationPlugin} that identifies itself by a unique string name.
 * Used for dynamic plugin resolution in service-oriented architectures.
 *
 * @param <T> The base type of entities (must implement {@link Mortal}).
 */
@EngineAPI
public interface NamedSimulationPlugin<T extends Mortal> extends SimulationPlugin<T> {
    /**
     * @return the unique name of the simulation plugin (e.g., "nature").
     */
    String getPluginName();
}
