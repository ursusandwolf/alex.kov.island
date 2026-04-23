package com.island.engine;

import com.island.model.Island;
import com.island.model.Cell;
import com.island.content.Plant;
import java.util.List;
import java.util.ArrayList;

public class PlantGrowthService implements Runnable {
    private final Island island;

    public PlantGrowthService(Island island) {
        this.island = island;
    }

    @Override
    public void run() {
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                List<Plant> newPlants = new ArrayList<>();
                cell.getPlants().forEach(p -> {
                    Plant child = p.reproduce();
                    if (child != null) newPlants.add(child);
                });
                newPlants.forEach(cell::addPlant);
            }
        }
    }
}
