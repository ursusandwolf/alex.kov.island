package com.island.nature.config;

import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesRegistry;
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
