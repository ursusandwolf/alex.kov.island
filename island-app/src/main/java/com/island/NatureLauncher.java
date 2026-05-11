package com.island;

import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.scheduling.GameLoop;
import com.island.nature.entities.core.Organism;

/**
 * Main entry point for the Nature Simulation (Island).
 */
@Slf4j
public class NatureLauncher {
    public static void main(String[] args) {
        Configuration config = Configuration.load();
        for (String arg : args) {
            if ("--headless".equals(arg)) {
                config.setHeadless(true);
            }
        }
        
        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        
        log.info("Starting Island Simulation...");
        log.info("Time limit: {} ms. Stop condition: extinction of all animals.", config.getMaxSimulationDurationMs());
        
        SimulationConfig simConfig = SimulationConfig.defaultFor(4);
        SimulationContext<Organism> context = engine.start(plugin, simConfig);

        CountDownLatch latch = new CountDownLatch(1);
        ScheduledExecutorService monitorService = Executors.newSingleThreadScheduledExecutor();

        // Monitoring extinction and duration
        monitor(context, latch, monitorService, engine, plugin, config);

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Simulation interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            monitorService.shutdown();
        }
        
        log.info("Simulation completed.");
    }

    private static void monitor(SimulationContext<Organism> context, CountDownLatch latch, ScheduledExecutorService scheduler, 
                                SimulationEngine<Organism> engine, NaturePlugin plugin, Configuration config) {
        GameLoop<Organism> gameLoop = context.gameLoop();
        long startTime = System.currentTimeMillis();
        long maxDurationMs = config.getMaxSimulationDurationMs();

        scheduler.scheduleAtFixedRate(() -> {
            if (!gameLoop.isRunning()) {
                latch.countDown();
                return;
            }

            if (System.currentTimeMillis() - startTime > maxDurationMs) {
                log.warn("\n⏳ Time limit reached ({} ms). Stopping simulation...", maxDurationMs);
                engine.stop(context, plugin);
                latch.countDown();
                return;
            }
        }, config.getMonitoringIntervalMs(), config.getMonitoringIntervalMs(), TimeUnit.MILLISECONDS);
    }
}
