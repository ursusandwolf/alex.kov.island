package com.island.config;

import com.island.engine.core.NamedSimulationPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * Spring configuration for simulation beans.
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SimulationProperties.class)
public class SimulationBeanConfig {

}
