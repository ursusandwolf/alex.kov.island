package com.island.config;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.model.Mortal;
import com.island.nature.NaturePlugin;
import com.island.simcity.SimCityPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring configuration for simulation beans.
 */
@Configuration
public class SimulationBeanConfig {

    /**
     * Provides the Nature plugin bean when the 'nature' profile is active.
     *
     * @param width  island width
     * @param height island height
     * @return the NaturePlugin instance
     */
    @Bean
    @Profile("nature")
    public SimulationPlugin<? extends Mortal> naturePlugin(
            @Value("${sim.width:20}") int width,
            @Value("${sim.height:20}") int height) {
        com.island.nature.config.Configuration cfg = com.island.nature.config.Configuration.load();
        cfg.setIslandWidth(width);
        cfg.setIslandHeight(height);
        return new NaturePlugin(cfg);
    }

    /**
     * Provides the SimCity plugin bean when the 'simcity' profile is active.
     *
     * @return the SimCityPlugin instance
     */
    @Bean
    @Profile("simcity")
    public SimulationPlugin<? extends Mortal> simCityPlugin() {
        return new SimCityPlugin();
    }

    /**
     * Provides the global simulation configuration.
     *
     * @param threads number of threads for parallel execution
     * @param tickMs  duration of a single tick in milliseconds
     * @return the SimulationConfig instance
     */
    @Bean
    public SimulationConfig simulationConfig(
            @Value("${sim.threads:4}") int threads,
            @Value("${sim.tickMs:100}") int tickMs) {
        return SimulationConfig.builder()
                .threadCount(threads)
                .tickDurationMs(tickMs)
                .build();
    }

    /**
     * Provides the simulation context as a managed bean.
     *
     * @param plugin current active plugin
     * @param config simulation configuration
     * @return the built SimulationContext
     */
    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SimulationContext<? extends Mortal> simulationContext(
            SimulationPlugin plugin,
            SimulationConfig config) {
        return new SimulationEngine().build(plugin, config);
    }
}
