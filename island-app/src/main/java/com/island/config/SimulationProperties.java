package com.island.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "sim")
public class SimulationProperties {
    private int width = 20;
    private int height = 20;
    private int threads = 4;
    private int tickMs = 100;
    private String defaultPlugin = "nature";
}
