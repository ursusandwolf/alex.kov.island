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

import com.island.engine.ecs.Component;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectivityService extends AbstractSimCityService {
    private final CityMap map;
    private final Queue<CityTile> queue = new ArrayDeque<>();
    private BitSet visited;

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        int width = map.getWidth();
        int height = map.getHeight();
        int totalTiles = width * height;
        
        if (visited == null || visited.size() < totalTiles) {
            visited = new BitSet(totalTiles);
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                CityTile tile = map.getGrid()[x][y];
                tile.setConnected(false);
                tile.setWatered(false);
                tile.setRailed(false);
                tile.setMetroConnected(false);
                tile.setPowered(false);
            }
        }

        propagateNetwork(BuildingComponent.Type.ROAD, CityTile::setConnected);
        propagateNetwork(BuildingComponent.Type.WATER_PIPE, CityTile::setWatered);
        propagateNetwork(BuildingComponent.Type.RAILWAY, CityTile::setRailed);
        propagateNetwork(BuildingComponent.Type.METRO, CityTile::setMetroConnected);
        propagateElectricity();
    }

    private void propagateElectricity() {
        queue.clear();
        visited.clear();

        int width = map.getWidth();
        int height = map.getHeight();

        // Find all power plants as sources
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                CityTile tile = map.getGrid()[x][y];
                if (hasInfrastructure(tile, BuildingComponent.Type.POWER_PLANT)) {
                    queue.add(tile);
                    visited.set(tile.getY() * width + tile.getX());
                }
            }
        }

        while (!queue.isEmpty()) {
            CityTile current = queue.poll();
            current.setPowered(true);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) continue;

                    int nx = current.getX() + dx;
                    int ny = current.getY() + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        CityTile neighbor = map.getGrid()[nx][ny];
                        int idx = ny * width + nx;
                        if (!visited.get(idx) && isConductive(neighbor)) {
                            visited.set(idx);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    private boolean isConductive(CityTile tile) {
        List<SimEntity> entities = tile.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            BuildingComponent b = entities.get(i).getComponent(BuildingComponent.class);
            if (b == null) continue;
            return switch (b.getType()) {
                case ROAD, RAILWAY, METRO, WATER_PIPE -> false;
                default -> true;
            };
        }
        return false;
    }

    private void propagateNetwork(BuildingComponent.Type type, java.util.function.BiConsumer<CityTile, Boolean> setter) {
        queue.clear();
        visited.clear();
        
        int width = map.getWidth();
        int height = map.getHeight();

        CityTile start = map.getGrid()[0][0];
        if (hasInfrastructure(start, type)) {
            queue.add(start);
            visited.set(0); // 0 * width + 0
        }

        while (!queue.isEmpty()) {
            CityTile current = queue.poll();
            setter.accept(current, true);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) continue;
                    
                    int nx = current.getX() + dx;
                    int ny = current.getY() + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        CityTile neighbor = map.getGrid()[nx][ny];
                        int idx = ny * width + nx;
                        if (!visited.get(idx)) {
                            if (hasInfrastructure(neighbor, type)) {
                                visited.set(idx);
                                queue.add(neighbor);
                            } else {
                                setter.accept(neighbor, true);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasInfrastructure(CityTile tile, BuildingComponent.Type type) {
        List<SimEntity> entities = tile.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            BuildingComponent b = entities.get(i).getComponent(BuildingComponent.class);
            if (b != null && b.getType() == type) return true;
        }
        return false;
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
    }
}
