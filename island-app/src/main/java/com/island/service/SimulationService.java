package com.island.service;

import com.island.engine.core.NamedSimulationPlugin;
import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.SimulationStatus;
import com.island.config.SimulationProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service to manage the simulation lifecycle and its dynamic context.
 */
@Service
@Slf4j
public class SimulationService {

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, NamedSimulationPlugin<?>> plugins;
    private final SimulationProperties properties;

    private volatile SimulationContext<?> context;

    public SimulationService(ApplicationEventPublisher eventPublisher, List<NamedSimulationPlugin<?>> pluginList, SimulationProperties properties) {
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.plugins = pluginList.stream()
                .collect(Collectors.toMap(p -> p.getPluginName().toLowerCase(), p -> p));
        log.info("Registered plugins: {}", plugins.keySet());
    }

    /**
     * Starts the simulation automatically with default configuration.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void startDefault() {
        start(properties.getDefaultPlugin(), properties.getWidth(), properties.getHeight(), properties.getTickMs());
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
            SimulationContext<?> oldContext = this.context;
            this.context = null; // Set to null before closing to prevent race conditions in snapshot/status calls
            oldContext.close();
            log.info("Previous simulation context destroyed");
        }

        SimulationConfig config = SimulationConfig.builder()
                .threadCount(properties.getThreads())
                .tickDurationMs(tickMs)
                .build();

        NamedSimulationPlugin<?> factory = plugins.get(type.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown simulation type: " + type);
        }

        // Create a configured instance of the plugin
        SimulationPlugin<?> plugin = factory.withConfiguration(width, height, initialSnapshot);

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
        SimulationContext<?> current = this.context;
        if (current != null) {
            current.gameLoop().stop();
            log.info("Simulation stopped");
        }
    }

    /**
     * Pauses the simulation game loop.
     */
    public void pause() {
        SimulationContext<?> current = this.context;
        if (current != null) {
            current.gameLoop().pause();
            log.info("Simulation paused");
        }
    }

    /**
     * Resumes the simulation game loop.
     */
    public void resume() {
        SimulationContext<?> current = this.context;
        if (current != null) {
            current.gameLoop().resume();
            log.info("Simulation resumed");
        }
    }

    /**
     * Returns the current status of the simulation.
     * 
     * @return the simulation status
     */
    public SimulationStatus getStatus() {
        SimulationContext<?> current = this.context;
        return current != null ? current.gameLoop().getStatus() : SimulationStatus.IDLE;
    }

    /**
     * Creates a current snapshot of the simulation world.
     * 
     * @return the world snapshot
     */
    public Optional<WorldSnapshot> getSnapshot() {
        return Optional.ofNullable(context).map(c -> c.world().createSnapshot());
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