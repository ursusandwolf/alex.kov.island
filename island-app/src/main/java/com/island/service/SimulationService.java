package com.island.service;

import com.island.engine.core.NamedSimulationPlugin;
import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.SimulationStatus;
import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to manage the simulation lifecycle and its dynamic context.
 */
@Service
@Slf4j
public class SimulationService {

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, NamedSimulationPlugin<?>> plugins;

    @Value("${sim.width:20}")
    private int defaultWidth;

    @Value("${sim.height:20}")
    private int defaultHeight;

    @Value("${sim.threads:4}")
    private int defaultThreads;

    @Value("${sim.tickMs:100}")
    private int defaultTickMs;

    @Value("${sim.default-plugin:nature}")
    private String defaultPlugin;

    private volatile SimulationContext<?> context;

    public SimulationService(ApplicationEventPublisher eventPublisher, List<NamedSimulationPlugin<?>> pluginList) {
        this.eventPublisher = eventPublisher;
        this.plugins = pluginList.stream()
                .collect(Collectors.toMap(p -> p.getPluginName().toLowerCase(), p -> p));
        log.info("Registered plugins: {}", plugins.keySet());
    }

    /**
     * Starts the simulation automatically with default configuration.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void startDefault() {
        start(defaultPlugin, defaultWidth, defaultHeight, defaultTickMs);
    }

    /**
     * Starts a new simulation with custom parameters, destroying the old one if it exists.
     *
     * @param type    the simulation type ("nature" or "simcity")
     * @param width   the grid width
     * @param height  the grid height
     * @param tickMs  the tick duration in milliseconds
     */
    public synchronized void start(String type, int width, int height, int tickMs) {
        doStart(type, width, height, tickMs, null);
    }

    /**
     * Starts a new simulation from an existing snapshot.
     */
    public synchronized void startFromSnapshot(WorldSnapshot snapshot, String type, int tickMs) {
        doStart(type, snapshot.getWidth(), snapshot.getHeight(), tickMs, snapshot);
    }

    private void doStart(String type, int width, int height, int tickMs, WorldSnapshot initialSnapshot) {
        if (context != null) {
            context.close();
            log.info("Previous simulation context destroyed");
        }

        SimulationConfig config = SimulationConfig.builder()
                .threadCount(defaultThreads)
                .tickDurationMs(tickMs)
                .build();

        NamedSimulationPlugin<?> plugin = plugins.get(type.toLowerCase());
        if (plugin == null) {
            throw new IllegalArgumentException("Unknown simulation type: " + type);
        }

        // Handle specific initial config for Nature if needed, or pass plugin instance directly
        // Currently assumes plugin can handle its own initialization state from the snapshot.
        this.context = new SimulationEngine().build(plugin, config);
        
        eventPublisher.publishEvent(new SimulationStartedEvent(this.context));
        
        this.context.gameLoop().start();
        log.info("Started new '{}' simulation ({}x{}) at {}ms/tick{}", 
                 type, width, height, tickMs, initialSnapshot != null ? " from snapshot" : "");
    }

    /**
     * Stops the simulation game loop.
     */
    public void stop() {
        if (context != null) {
            context.gameLoop().stop();
            log.info("Simulation stopped");
        }
    }

    /**
     * Pauses the simulation game loop.
     */
    public void pause() {
        if (context != null) {
            context.gameLoop().pause();
            log.info("Simulation paused");
        }
    }

    /**
     * Resumes the simulation game loop.
     */
    public void resume() {
        if (context != null) {
            context.gameLoop().resume();
            log.info("Simulation resumed");
        }
    }

    /**
     * Returns the current status of the simulation.
     * 
     * @return the simulation status
     */
    public SimulationStatus getStatus() {
        return context != null ? context.gameLoop().getStatus() : SimulationStatus.IDLE;
    }

    /**
     * Creates a current snapshot of the simulation world.
     * 
     * @return the world snapshot, or null if no simulation is running
     */
    public WorldSnapshot getSnapshot() {
        return context != null ? context.world().createSnapshot() : null;
    }

    /**
     * Ensures proper cleanup of simulation resources on application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        if (context != null) {
            context.close();
            log.info("Simulation shutdown complete");
        }
    }
}
