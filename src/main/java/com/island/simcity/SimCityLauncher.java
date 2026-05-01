package com.island.simcity;

import com.island.engine.GameLoop;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import com.island.simcity.service.ConnectivityService;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;
import com.island.simcity.view.CityConsoleView;

public class SimCityLauncher {
    public static void main(String[] args) {
        System.out.println("Starting SimCity Simulation...");

        // 1. Initialize World
        CityMap map = new CityMap(10, 10);

        // 2. Initialize Tasks
        ConnectivityService connService = new ConnectivityService(map);
        PopulationService popService = new PopulationService();
        EconomyService economyService = new EconomyService(map);
        CityConsoleView view = new CityConsoleView();

        // 3. Register with GameLoop
        GameLoop<SimEntity> gameLoop = new GameLoop<>(100, 4);
        gameLoop.setWorld(map);
        gameLoop.addRecurringTask(connService); // Connectivity must run first
        gameLoop.addRecurringTask(popService);
        gameLoop.addRecurringTask(economyService);
        
        // Cleanup task
        gameLoop.addRecurringTask(t -> {
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    map.getGrid()[x][y].cleanupDeadEntities(e -> { });
                }
            }
        });

        // 4. Create a road network and zones
        // Road from (0,0) to (5,0)
        for (int x = 0; x <= 5; x++) {
            map.getGrid()[x][0].addEntity(new Building(Building.Type.ROAD));
        }
        
        // Road branch to (2,5)
        for (int y = 1; y <= 5; y++) {
            map.getGrid()[2][y].addEntity(new Building(Building.Type.ROAD));
        }

        // Residential zones next to road
        map.getGrid()[0][1].addEntity(new Building(Building.Type.RESIDENTIAL));
        map.getGrid()[1][1].addEntity(new Building(Building.Type.RESIDENTIAL));
        map.getGrid()[3][1].addEntity(new Building(Building.Type.RESIDENTIAL));
        
        // Industrial zone next to road
        map.getGrid()[2][6].addEntity(new Building(Building.Type.INDUSTRIAL));
        
        // Isolated zone (no connectivity)
        map.getGrid()[9][9].addEntity(new Building(Building.Type.RESIDENTIAL));

        // 4b. Setup neighbors for BFS and happiness logic
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                CityTile tile = map.getGrid()[x][y];
                java.util.List<com.island.engine.SimulationNode<SimEntity>> neighbors = new java.util.ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        map.getNode(tile, dx, dy).ifPresent(neighbors::add);
                    }
                }
                tile.setNeighbors(neighbors);
            }
        }

        // 5. Run simulation
        for (int i = 0; i < 30; i++) {
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
