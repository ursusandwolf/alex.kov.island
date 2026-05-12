package com.island.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.island.engine.model.WorldSnapshot;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for simulation Jackson serialization using the recommended customizer approach.
 */
@Configuration
public class SimulationJacksonConfig {

    /**
     * Customizes the default Spring Boot ObjectMapper with simulation-specific mixins and settings.
     * 
     * @return the Jackson customizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer simulationJsonCustomizer() {
        return builder -> builder
                .mixIn(WorldSnapshot.class, WorldSnapshotMixin.class)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                );
    }
}
