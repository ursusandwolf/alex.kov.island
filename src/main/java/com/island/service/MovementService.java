package com.island.service;
import com.island.util.RandomUtils;
import com.island.content.Animal;
import com.island.content.DeathCause;
import com.island.model.Cell;
import com.island.model.Island;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

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
            if (animal.isAlive()) {
                if (!animal.move()) {
                    if (!animal.isAlive()) {
                        island.reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                    }
                    continue;
                }
                
                int speed = animal.getSpeed();
                
                // --- Red Book Mobility Bonus ---
                int currentCount = island.getSpeciesCount(animal.getSpeciesKey());
                int globalCapacity = islandArea * animal.getMaxPerCell();
                if (currentCount < globalCapacity * 0.05) {
                    speed += 2; 
                }

                if (speed > 0) {
                    int dx = ThreadLocalRandom.current().nextInt(-speed, speed + 1);
                    int dy = ThreadLocalRandom.current().nextInt(-speed, speed + 1);
                    
                    Cell target = island.getCell(cell.getX() + dx, cell.getY() + dy);
                    if (target != cell) {
                        island.moveOrganism(animal, cell, target);
                    }
                }
            }
        }
    }
}
