package com.island.engine;

import com.island.content.SpeciesKey;
import java.util.Map;

/**
 * Main entry point for the simulation.
 */
public class SimulatorMain {
    public static void main(String[] args) {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup();
        
        GameLoop gameLoop = context.getGameLoop();
        
        System.out.println("Запуск симуляции острова...");
        System.out.println("Лимит времени: 5 минут. Условие остановки: вымирание любого вида.");
        
        gameLoop.start();

        // Monitoring extinction and duration
        monitor(context);
    }

    private static void monitor(SimulationContext context) {
        GameLoop gameLoop = context.getGameLoop();
        long startTime = System.currentTimeMillis();
        long maxDurationMs = 5 * 60 * 1000;

        try {
            while (gameLoop.isRunning()) {
                Thread.sleep(2000);

                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    System.out.println("\n⏳ Время вышло (5 минут). Остановка симуляции...");
                    gameLoop.stop();
                    break;
                }

                Map<SpeciesKey, Integer> counts = context.getIsland().getSpeciesCounts();
                for (SpeciesKey species : context.getSpeciesRegistry().getAllAnimalKeys()) {
                    boolean isBiomass = context.getSpeciesRegistry().getAnimalType(species)
                            .map(com.island.content.AnimalType::isBiomass).orElse(false);
                    if (isBiomass) {
                        continue;
                    }
                    if (counts.getOrDefault(species, 0) == 0) {
                        System.out.println("\n💀 Вид '" + species.getCode() + "' вымер! Остановка симуляции...");
                        gameLoop.stop();
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Симуляция завершена.");
    }
}
