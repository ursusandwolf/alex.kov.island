package com.island.service;

import com.island.engine.core.SimulationContext;
import com.island.engine.scheduling.SimulationStatus;
import com.island.engine.model.WorldSnapshot;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service to manage the simulation lifecycle using Spring-injected context.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationContext<?> context;

    /**
     * Starts the simulation automatically when the application is ready.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void start() {
        context.gameLoop().start();
        log.info("Simulation started");
    }

    /**
     * Explicitly starts the simulation.
     */
    public void startExplicitly() {
        context.gameLoop().start();
    }

    /**
     * Stops the simulation game loop.
     */
    public void stop() {
        context.gameLoop().stop();
        log.info("Simulation stopped");
    }

    /**
     * Pauses the simulation game loop.
     */
    public void pause() {
        context.gameLoop().pause();
        log.info("Simulation paused");
    }

    /**
     * Resumes the simulation game loop.
     */
    public void resume() {
        context.gameLoop().resume();
        log.info("Simulation resumed");
    }

    /**
     * Returns the current status of the simulation.
     * 
     * @return the simulation status
     */
    public SimulationStatus getStatus() {
        return context.gameLoop().getStatus();
    }

    /**
     * Creates a current snapshot of the simulation world.
     * 
     * @return the world snapshot
     */
    public WorldSnapshot getSnapshot() {
        return context.world().createSnapshot();
    }

    /**
     * Ensures proper cleanup of simulation resources on application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        context.close();
        log.info("Simulation shutdown complete");
    }
}
