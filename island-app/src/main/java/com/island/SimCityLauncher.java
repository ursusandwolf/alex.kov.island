package com.island;

import com.island.simcity.SimCityPlugin;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.service.BuildingService;
import com.island.simcity.view.CityConsoleView;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;

public class SimCityLauncher {
    public static void main(String[] args) {
        System.out.println("Starting SimCity Simulation...");

        SimCityPlugin plugin = new SimCityPlugin(10, 10);
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        
        // Build engine (which calls initialize and registerTasks)
        SimulationContext<SimEntity> context = engine.build(plugin, 100, 4);
        CityMap map = (CityMap) context.world();
        BuildingService buildingService = new BuildingService(map);
        CityConsoleView view = new CityConsoleView();

        // Road from (0,0) to (5,0)
        for (int x = 0; x <= 5; x++) {
            buildingService.build(x, 0, BuildingComponent.Type.ROAD);
        }
        
        // Road branch to (2,5)
        for (int y = 1; y <= 5; y++) {
            buildingService.build(2, y, BuildingComponent.Type.ROAD);
        }

        // Residential zones next to road
        buildingService.build(0, 1, BuildingComponent.Type.RESIDENTIAL);
        buildingService.build(1, 1, BuildingComponent.Type.RESIDENTIAL);
        buildingService.build(3, 1, BuildingComponent.Type.RESIDENTIAL);
        
        // Industrial zone next to road
        buildingService.build(2, 6, BuildingComponent.Type.INDUSTRIAL);
        
        // Isolated zone (no connectivity)
        buildingService.build(9, 9, BuildingComponent.Type.RESIDENTIAL);

        // 5. Run simulation
        for (int i = 0; i < 30; i++) {
            // Dynamic player actions
            if (i == 10) {
                System.out.println("PLAYER ACTION: Increasing taxes to 40%");
                map.setTaxRate(40);
            }
            if (i == 20) {
                System.out.println("PLAYER ACTION: Reducing taxes to 10%");
                map.setTaxRate(10);
                System.out.println("PLAYER ACTION: Building more residential...");
                buildingService.build(4, 1, BuildingComponent.Type.RESIDENTIAL);
                buildingService.build(5, 1, BuildingComponent.Type.RESIDENTIAL);
            }

            context.gameLoop().runTick();
            view.render(map, i + 1);
            printHappiness(map);
        }

        System.out.println("SimCity Simulation Finished.");
    }

    private static void printHappiness(CityMap map) {
        double avgHappiness = 0;
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                for (SimEntity e : map.getGrid()[x][y].getEntities()) {
                    PopulationComponent pop = e.getComponent(PopulationComponent.class);
                    if (pop != null) {
                        avgHappiness += pop.getHappiness();
                        count++;
                    }
                }
            }
        }
        if (count > 0) {
            System.out.printf("Average Happiness: %.2f%%%n", avgHappiness / count);
        }
    }
}