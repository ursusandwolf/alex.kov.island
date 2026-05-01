package com.island.nature;

import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SimulationBootstrap;
import com.island.nature.entities.SpeciesKey;
import com.island.engine.GameLoop;
import com.island.engine.SimulationContext;
import com.island.nature.model.Island;
import java.util.Map;

/**
 * Main entry point for the Nature Simulation (Island).
 */
public class NatureLauncher {
    public static void main(String[] args) {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext<Organism> context = bootstrap.setup();
        
        GameLoop<Organism> gameLoop = context.getGameLoop();
        
        System.out.println("Запуск симуляции острова...");
        System.out.println("Лимит времени: 5 минут. Условие остановки: вымирание любого вида.");
        
        gameLoop.start();

        // Monitoring extinction and duration
        monitor(context);
    }

    private static void monitor(SimulationContext<Organism> context) {
        GameLoop<Organism> gameLoop = context.getGameLoop();
        long startTime = System.currentTimeMillis();
        long maxDurationMs = 5 * 60 * 1000;
        
        Island island = (Island) context.getWorld();

        try {
            while (gameLoop.isRunning()) {
                Thread.sleep(2000);

                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    System.out.println("\n⏳ Время вышло (5 минут). Остановка симуляции...");
                    gameLoop.stop();
                    break;
                }

                Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
                for (SpeciesKey species : island.getRegistry().getAllAnimalKeys()) {
                    boolean isBiomass = island.getRegistry().getAnimalType(species)
                            .map(AnimalType::isBiomass).orElse(false);
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
