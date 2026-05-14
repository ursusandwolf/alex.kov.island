package com.island.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "sim")
public class SimulationProperties {
    
    @Min(5)
    @Max(200)
    private int width = 20;
    
    @Min(5)
    @Max(200)
    private int height = 20;
    
    @Min(1)
    @Max(32)
    private int threads = 4;
    
    @Min(10)
    @Max(10000)
    private int tickMs = 100;
    
    @NotBlank
    private String defaultPlugin = "nature";

    @Min(1)
    @Max(100)
    private int broadcastInterval = 5;

    @Min(10)
    @Max(5000)
    private int broadcastRateMs = 100;
}
