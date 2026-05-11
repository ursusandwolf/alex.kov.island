package com.island.service;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.SimulationStatus;
import com.island.engine.model.Mortal;
import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.simcity.SimCityPlugin;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Service to manage the simulation lifecycle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {
    private final SimulationEngine<Mortal> engine = new SimulationEngine<>();
    private final AtomicReference<SimulationContext<Mortal>> contextRef = new AtomicReference<>();
    private final AtomicReference<SimulationPlugin> pluginRef = new AtomicReference<>();
    
    private final SnapshotBroadcaster broadcaster;

    @SuppressWarnings("unchecked")
    public void startSimulation(String type) {
        if (contextRef.get() != null && contextRef.get().gameLoop().isRunning()) {
            throw new IllegalStateException("Simulation is already running");
        }

        SimulationPlugin plugin;
        if ("nature".equalsIgnoreCase(type)) {
            plugin = new NaturePlugin(Configuration.load());
        } else if ("simcity".equalsIgnoreCase(type)) {
            plugin = new SimCityPlugin();
        } else {
            throw new IllegalArgumentException("Unknown simulation type: " + type);
        }

        SimulationConfig simConfig = SimulationConfig.defaultFor(4);
        SimulationContext<Mortal> context = (SimulationContext<Mortal>) engine.start((SimulationPlugin) plugin, simConfig);
        
        broadcaster.setContext(context);
        context.gameLoop().addRecurringTask(broadcaster);
        
        contextRef.set(context);
        pluginRef.set(plugin);
        log.info("Simulation {} started with WebSocket broadcasting", type);
    }

    public void pause() {
        SimulationContext<Mortal> context = contextRef.get();
        if (context != null) {
            context.gameLoop().pause();
        }
    }

    public void resume() {
        SimulationContext<Mortal> context = contextRef.get();
        if (context != null) {
            context.gameLoop().resume();
        }
    }

    public void stop() {
        SimulationContext<Mortal> context = contextRef.get();
        SimulationPlugin plugin = pluginRef.get();
        if (context != null && plugin != null) {
            engine.stop(context, plugin);
            contextRef.set(null);
            pluginRef.set(null);
            log.info("Simulation stopped");
        }
    }

    public SimulationStatus getStatus() {
        SimulationContext<Mortal> context = contextRef.get();
        if (context == null) return SimulationStatus.IDLE;
        return context.gameLoop().getStatus();
    }

    @PreDestroy
    public void cleanup() {
        stop();
    }
}
