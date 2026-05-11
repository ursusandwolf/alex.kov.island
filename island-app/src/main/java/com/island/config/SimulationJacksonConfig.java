package com.island.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.engine.model.WorldSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for simulation Jackson serialization.
 */
@Configuration
public class SimulationJacksonConfig {

    /**
     * Provides a pre-configured ObjectMapper bean with necessary mixins for simulation models.
     * 
     * @return the configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(WorldSnapshot.class, WorldSnapshotMixin.class);
        return mapper;
    }
}
