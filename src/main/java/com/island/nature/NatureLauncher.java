package com.island.nature;

import com.island.engine.GameLoop;
import com.island.engine.SimulationEngine;
import com.island.engine.SimulationContext;
import com.island.nature.config.Configuration;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.model.Island;
import com.island.nature.view.ConsoleView;
import java.util.Map;
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
        
        log.info("Запуск симуляции острова...");
        log.info("Лимит времени: 5 минут. Условие остановки: вымирание любого вида.");
        
        SimulationContext<Organism> context = engine.start(plugin, config.getTickDurationMs(), 4, view);

        CountDownLatch latch = new CountDownLatch(1);
        ScheduledExecutorService monitorService = Executors.newSingleThreadScheduledExecutor();

        // Monitoring extinction and duration
        monitor(context, latch, monitorService);

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Симуляция прервана", e);
            Thread.currentThread().interrupt();
        } finally {
            monitorService.shutdown();
        }
        
        log.info("Симуляция завершена.");
    }

    private static void monitor(SimulationContext<Organism> context, CountDownLatch latch, ScheduledExecutorService scheduler) {
        GameLoop<Organism> gameLoop = context.getGameLoop();
        long startTime = System.currentTimeMillis();
        long maxDurationMs = 5 * 60 * 1000;
        
        Island island = (Island) context.getWorld();

        scheduler.scheduleAtFixedRate(() -> {
            if (!gameLoop.isRunning()) {
                latch.countDown();
                return;
            }

            if (System.currentTimeMillis() - startTime > maxDurationMs) {
                log.warn("\n⏳ Время вышло (5 минут). Остановка симуляции...");
                gameLoop.stop();
                latch.countDown();
                return;
            }

            Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
            for (SpeciesKey species : island.getRegistry().getAllAnimalKeys()) {
                boolean isBiomass = island.getRegistry().getAnimalType(species)
                        .map(AnimalType::isBiomass).orElse(false);
                if (isBiomass) {
                    continue;
                }
                if (counts.getOrDefault(species, 0) == 0) {
                    log.error("\n💀 Вид '{}' вымер! Остановка симуляции...", species.getCode());
                    gameLoop.stop();
                    latch.countDown();
                    return;
                }
            }
        }, 2, 2, TimeUnit.SECONDS);
    }
}
