package com.island.simcity;

import com.island.engine.GameLoop;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.service.BuildingService;
import com.island.simcity.service.CityAnalyticsService;
import com.island.simcity.service.ConnectivityService;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;
import com.island.simcity.view.CityConsoleView;

public class SimCityLauncher {
    public static void main(String[] args) {
        System.out.println("Starting SimCity Simulation...");

        // 1. Initialize World
        CityMap map = new CityMap(10, 10);
        map.initialize();

        // 2. Initialize Tasks
        ConnectivityService connService = new ConnectivityService(map);
        CityAnalyticsService analyticsService = new CityAnalyticsService(map);
        PopulationService popService = new PopulationService(map);
        EconomyService economyService = new EconomyService(map);
        BuildingService buildingService = new BuildingService(map);
        CityConsoleView view = new CityConsoleView();

        // 3. Register with GameLoop
        GameLoop<SimEntity> gameLoop = new GameLoop<>(100, 4);
        gameLoop.setWorld(map);
        gameLoop.addRecurringTask(connService); // 1. Connectivity
        gameLoop.addRecurringTask(analyticsService); // 2. Analytics (Demand/Pop/Jobs)
        gameLoop.addRecurringTask(popService); // 3. Growth/Migration
        gameLoop.addRecurringTask(economyService); // 4. Taxes/Maintenance

        // Cleanup task
        gameLoop.addRecurringTask(t -> {
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    map.getGrid()[x][y].cleanupDeadEntities(e -> { });
                }
            }
        });

        // 4. Create a road network and zones using BuildingService
        // Road from (0,0) to (5,0)
        for (int x = 0; x <= 5; x++) {
            buildingService.build(x, 0, Building.Type.ROAD);
        }
        
        // Road branch to (2,5)
        for (int y = 1; y <= 5; y++) {
            buildingService.build(2, y, Building.Type.ROAD);
        }

        // Residential zones next to road
        buildingService.build(0, 1, Building.Type.RESIDENTIAL);
        buildingService.build(1, 1, Building.Type.RESIDENTIAL);
        buildingService.build(3, 1, Building.Type.RESIDENTIAL);
        
        // Industrial zone next to road
        buildingService.build(2, 6, Building.Type.INDUSTRIAL);
        
        // Isolated zone (no connectivity)
        buildingService.build(9, 9, Building.Type.RESIDENTIAL);

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
                buildingService.build(4, 1, Building.Type.RESIDENTIAL);
                buildingService.build(5, 1, Building.Type.RESIDENTIAL);
            }

            gameLoop.runTick();
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
                    if (e instanceof Resident r) {
                        avgHappiness += r.getHappiness();
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
