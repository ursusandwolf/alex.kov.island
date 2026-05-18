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
    private volatile int width = 20;
    
    @Min(5)
    @Max(200)
    private volatile int height = 20;
    
    @Min(1)
    @Max(32)
    private volatile int threads = 4;
    
    @Min(10)
    @Max(10000)
    private volatile int tickMs = 100;
    
    @NotBlank
    private volatile String defaultPlugin = "nature";

    @NotBlank
    private volatile String historyDir = "data/snapshots";

    @Min(1)
    @Max(100)
    private volatile int broadcastInterval = 5;

    @Min(10)
    @Max(5000)
    private volatile int broadcastRateMs = 100;
}
