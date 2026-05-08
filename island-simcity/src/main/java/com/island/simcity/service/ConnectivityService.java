package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectivityService extends AbstractSimCityService {
    private final CityMap map;

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                map.getGrid()[x][y].setConnected(false);
            }
        }

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
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) { // Only orthogonal for roads
                        continue;
                    }
                    int nx = current.getX() + dx;
                    int ny = current.getY() + dy;
                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        CityTile neighbor = map.getGrid()[nx][ny];
                        if (hasRoad(neighbor) && visited.add(neighbor)) {
                            queue.add(neighbor);
                        } else {
                            // Non-road tile connected to road is still connected
                            neighbor.setConnected(true);
                        }
                    }
                }
            }
        }
    }

    private boolean hasRoad(CityTile tile) {
        return tile.getEntities().stream()
                .anyMatch(e -> {
                    BuildingComponent b = e.getComponent(BuildingComponent.class);
                    return b != null && b.getType() == BuildingComponent.Type.ROAD;
                });
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
    }
}
