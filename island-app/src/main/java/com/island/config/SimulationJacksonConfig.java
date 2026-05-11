package com.island.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.engine.model.WorldSnapshot;

/**
 * Provides Jackson configuration for simulation snapshots.
 */
public final class SimulationJacksonConfig {
    
    private SimulationJacksonConfig() {}

    /**
     * Configures the provided ObjectMapper with necessary mixins for simulation models.
     * 
     * @param mapper the ObjectMapper to configure
     * @return the configured ObjectMapper
     */
    public static ObjectMapper configure(ObjectMapper mapper) {
        mapper.addMixIn(WorldSnapshot.class, WorldSnapshotMixin.class);
        return mapper;
    }

    /**
     * Creates a new ObjectMapper pre-configured for simulation models.
     * 
     * @return a new pre-configured ObjectMapper
     */
    public static ObjectMapper createMapper() {
        return configure(new ObjectMapper());
    }
}
