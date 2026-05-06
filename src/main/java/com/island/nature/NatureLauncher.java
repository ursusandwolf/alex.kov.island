package com.island.nature;

import com.island.engine.GameLoop;
import com.island.engine.SimulationEngine;
import com.island.engine.SimulationContext;
import com.island.nature.config.Configuration;
import com.island.nature.entities.Organism;
import com.island.nature.view.ConsoleView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the Nature Simulation (Island).
 */
@Slf4j
public class NatureLauncher {
    public static void main(String[] args) {
        Configuration config = Configuration.load();
        
        ConsoleView view = new ConsoleView();
        NaturePlugin plugin = new NaturePlugin(config, view);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        
        log.info("Starting Island Simulation...");
        log.info("Time limit: {} ms. Stop condition: extinction of any species.", config.getMaxSimulationDurationMs());
        
        SimulationContext<Organism> context = engine.start(plugin, config.getTickDurationMs(), 4);

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

            if (plugin.shouldStop(context)) {
                log.error("\n💀 Species extinction or critical condition detected! Stopping...");
                engine.stop(context, plugin);
                latch.countDown();
            }
        }, config.getMonitoringIntervalMs(), config.getMonitoringIntervalMs(), TimeUnit.MILLISECONDS);
    }
}
