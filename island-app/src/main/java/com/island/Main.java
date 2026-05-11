package com.island;

import com.island.engine.core.SimulationPlugin;
import java.util.ServiceLoader;

/**
 * Главная точка входа в симуляцию острова.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("--- Island Ecosystem Simulator ---");
        
        ServiceLoader<SimulationPlugin> loader = ServiceLoader.load(SimulationPlugin.class);
        long pluginCount = loader.stream().count();
        
        System.out.println("Discovered plugins: " + pluginCount);
        loader.forEach(plugin -> System.out.println(" - " + plugin.getClass().getName()));

        // По умолчанию запускаем NatureLauncher, если нет специфичных аргументов
        if (args.length > 0 && args[0].equalsIgnoreCase("--simcity")) {
            SimCityLauncher.main(args);
        } else {
            NatureLauncher.main(args);
        }
    }
}
