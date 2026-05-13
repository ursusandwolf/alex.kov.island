package com.island.engine.core;

import com.island.engine.model.Mortal;
import com.island.engine.model.WorldSnapshot;

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

    /**
     * Creates a new instance of the plugin configured with specific world parameters.
     * This allows Spring-managed singleton plugins to act as factories for specific simulation runs.
     * <p>
     * <b>Contract:</b> Implementations MUST return a new instance of the plugin to prevent
     * concurrent configuration pollution on singleton beans.
     *
     * @param width    the desired grid width
     * @param height   the desired grid height
     * @param snapshot an optional initial snapshot to load from
     * @return a new, configured simulation plugin instance
     */
    SimulationPlugin<T> withConfiguration(int width, int height, WorldSnapshot snapshot);
}
