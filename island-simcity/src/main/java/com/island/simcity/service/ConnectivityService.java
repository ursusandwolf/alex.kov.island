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

public class ConnectivityService extends AbstractSimCityService {
    private final CityMap map;

    public ConnectivityService(CityMap map) {
        this.map = map;
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                CityTile tile = map.getGrid()[x][y];
                tile.setConnected(false);
                tile.setWatered(false);
                tile.setRailed(false);
                tile.setMetroConnected(false);
            }
        }

        propagateNetwork(BuildingComponent.Type.ROAD, CityTile::setConnected);
        propagateNetwork(BuildingComponent.Type.WATER_PIPE, CityTile::setWatered);
        propagateNetwork(BuildingComponent.Type.RAILWAY, CityTile::setRailed);
        propagateNetwork(BuildingComponent.Type.METRO, CityTile::setMetroConnected);
    }

    private void propagateNetwork(BuildingComponent.Type type, java.util.function.BiConsumer<CityTile, Boolean> setter) {
        Queue<CityTile> queue = new ArrayDeque<>();
        Set<CityTile> visited = new HashSet<>();
        
        // Start from (0,0) as the city entrance/source for all networks for simplicity
        CityTile start = map.getGrid()[0][0];
        if (hasInfrastructure(start, type)) {
            queue.add(start);
            visited.add(start);
        }

        while (!queue.isEmpty()) {
            CityTile current = queue.poll();
            setter.accept(current, true);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) continue;
                    
                    int nx = current.getX() + dx;
                    int ny = current.getY() + dy;
                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        CityTile neighbor = map.getGrid()[nx][ny];
                        if (hasInfrastructure(neighbor, type) && visited.add(neighbor)) {
                            queue.add(neighbor);
                        } else {
                            // Any tile adjacent to the infrastructure is "connected" to it
                            setter.accept(neighbor, true);
                        }
                    }
                }
            }
        }
    }

    private boolean hasInfrastructure(CityTile tile, BuildingComponent.Type type) {
        return tile.getEntities().stream()
                .anyMatch(e -> {
                    BuildingComponent b = e.getComponent(BuildingComponent.class);
                    return b != null && b.getType() == type;
                });
    }

    private boolean hasRoad(CityTile tile) {
        return hasInfrastructure(tile, BuildingComponent.Type.ROAD);
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
    }
}
