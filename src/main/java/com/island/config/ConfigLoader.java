package com.island.config;

import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import com.island.util.InteractionMatrix;

/**
 * Handles loading of all simulation configurations.
 */
public class ConfigLoader {
    public Configuration loadGeneralConfig() {
        return Configuration.load();
    }

    public InteractionMatrix loadInteractionMatrix(SpeciesRegistry registry) {
        return InteractionMatrix.buildFrom(registry);
    }
}
