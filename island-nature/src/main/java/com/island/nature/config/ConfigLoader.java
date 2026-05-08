package com.island.nature.config;

import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.model.InteractionMatrix;


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