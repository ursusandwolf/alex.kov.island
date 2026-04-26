package com.island.service;
import com.island.util.RandomUtils;
import com.island.content.Animal;
import com.island.model.Cell;
import com.island.model.Island;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class MovementService extends AbstractService {

    public MovementService(Island island, ExecutorService executor) {
        super(island, executor);
    }

    @Override
    protected void processCell(Cell cell) {
        // Use a snapshot to avoid ConcurrentModificationException or missed animals
        List<Animal> animals = new java.util.ArrayList<>(cell.getAnimals());
        Island island = cell.getIsland();
        int islandArea = island.getWidth() * island.getHeight();

        for (Animal animal : animals) {
            if (animal.isAlive() && animal.move()) {
                int speed = animal.getSpeed();
                
                // --- Red Book Mobility Bonus ---
                // Endangered animals move 2 cells further to find mates/food
                int currentCount = island.getSpeciesCount(animal.getSpeciesKey());
                int globalCapacity = islandArea * animal.getMaxPerCell();
                if (currentCount > 0 && currentCount < globalCapacity * com.island.config.SimulationConstants.ENDANGERED_POPULATION_THRESHOLD) {
                    speed += 2; 
                }

                if (speed > 0) {
                    int dx = RandomUtils.nextInt(-speed, speed + 1);
                    int dy = RandomUtils.nextInt(-speed, speed + 1);
                    int tx = cell.getX() + dx;
                    int ty = cell.getY() + dy;
                    
                    Cell target = island.getCell(tx, ty);
                    if (target != cell) {
                        island.moveOrganism(animal, cell, target);
                    }
                }
            }
        }
    }
}
