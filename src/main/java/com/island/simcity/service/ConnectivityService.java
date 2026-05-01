package com.island.simcity.service;

import com.island.engine.CellService;
import com.island.engine.SimulationNode;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ConnectivityService implements CellService<SimEntity> {
    private final CityMap map;

    public ConnectivityService(CityMap map) {
        this.map = map;
    }

    @Override
    public void beforeTick(int tickCount) {
        // Reset connectivity
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                map.getGrid()[x][y].setConnected(false);
            }
        }

        // BFS to find all roads connected to the "City Entrance" at (0,0)
        Queue<CityTile> queue = new ArrayDeque<>();
        Set<CityTile> visited = new HashSet<>();

        CityTile start = map.getGrid()[0][0];
        if (hasRoad(start)) {
            queue.add(start);
            visited.add(start);
        }

        while (!queue.isEmpty()) {
            CityTile current = queue.poll();
            current.setConnected(true);

            // Mark neighbors of roads as "connected" to the road network
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    
                    int nx = current.getX() + dx;
                    int ny = current.getY() + dy;

                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        CityTile neighbor = map.getGrid()[nx][ny];
                        neighbor.setConnected(true); // Any tile next to a road is connected
                        
                        if (hasRoad(neighbor) && !visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    private boolean hasRoad(CityTile tile) {
        return tile.getEntities().stream()
                .anyMatch(e -> e instanceof Building b && b.getType() == Building.Type.ROAD);
    }

    @Override
    public void processCell(SimulationNode<SimEntity> node, int tickCount) {
        // Connectivity is handled in beforeTick for the whole map
    }
}
