package com.island.simcity.view;

import com.island.simcity.entities.Building;
import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;

public class CityConsoleView {
    public void render(CityMap map, int tick) {
        System.out.println("\n=== CITY DASHBOARD (Tick: " + tick + ") ===");
        System.out.printf("Population: %d | Money: %d (%+d)%n", 
                map.getPopulation(), map.getMoney(), (map.getLastTickIncome() - map.getLastTickExpenses()));
        System.out.printf("R Demand: %3d | C Demand: %3d | I Demand: %3d%n", 
                map.getResDemand(), map.getComDemand(), map.getIndDemand());
        System.out.println("Map:");
        
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                CityTile tile = map.getGrid()[x][y];
                String symbol = getTileSymbol(tile);
                System.out.print(symbol + " ");
            }
            System.out.println();
        }
        System.out.println("===========================================");
    }

    private String getTileSymbol(CityTile tile) {
        if (!tile.isConnected()) {
            return "x"; // Disconnected
        }
        
        Building building = tile.getEntities().stream()
                .filter(e -> e instanceof Building)
                .map(e -> (Building) e)
                .findFirst()
                .orElse(null);

        if (building == null) {
            return "."; // Empty connected tile
        }

        return switch (building.getType()) {
            case RESIDENTIAL -> "R";
            case COMMERCIAL -> "C";
            case INDUSTRIAL -> "I";
            case ROAD -> "#";
        };
    }

    private int getPopulation(CityMap map) {
        int total = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                total += (int) map.getGrid()[x][y].getEntities().stream()
                        .filter(e -> e instanceof Resident)
                        .count();
            }
        }
        return total;
    }
}
