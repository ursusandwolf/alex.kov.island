package com.island.service;

import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.model.Cell;
import com.island.model.Island;

import static com.island.config.SimulationConstants.BASE_MOVE_COST_PERCENT;
import static com.island.config.SimulationConstants.SPEED_MOVE_COST_STEP_PERCENT;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for animal and mobile biomass movement.
 */
public class MovementService extends AbstractService {

    public MovementService(Island island, ExecutorService executor) {
        super(island, executor);
    }

    @Override
    protected void processCell(Cell cell) {
        processAnimals(cell);
        processMobileBiomass(cell);
    }

    private void processAnimals(Cell cell) {
        // Use a snapshot to avoid ConcurrentModificationException
        List<Animal> animals = new java.util.ArrayList<>(cell.getAnimals());
        Island island = cell.getIsland();
        int islandArea = island.getWidth() * island.getHeight();

        for (Animal animal : animals) {
            if (animal.isAlive()) {
                if (!animal.canPerformAction()) {
                    continue;
                }

                double moveCost = animal.getMaxEnergy() 
                        * (BASE_MOVE_COST_PERCENT + (animal.getSpeed() * SPEED_MOVE_COST_STEP_PERCENT));
                animal.consumeEnergy(moveCost);

                if (!animal.isAlive()) {
                    island.reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                    continue;
                }
                
                int speed = animal.getSpeed();
                
                // --- Red Book Mobility Bonus ---
                int currentCount = island.getSpeciesCount(animal.getSpeciesKey());
                int globalCapacity = islandArea * animal.getMaxPerCell();
                if (currentCount > 0 && currentCount < globalCapacity * 0.05) {
                    speed += 2; 
                }

                if (speed > 0) {
                    Cell target = selectTargetCell(cell, speed);
                    if (target != cell) {
                        island.moveOrganism(animal, cell, target);
                    }
                }
            }
        }
    }

    private void processMobileBiomass(Cell cell) {
        List<Biomass> containers = cell.getBiomassContainers();
        Island island = cell.getIsland();

        for (Biomass b : containers) {
            if (b.isAlive() && b.getSpeed() > 0 && b.getBiomass() > 0) {
                // To avoid the entire biomass moving back and forth in the same tick if multiple cells are processed,
                // we could use a marker, but for simplicity we move the whole "swarm" once.
                Cell target = selectTargetCell(cell, b.getSpeed());
                if (target != cell) {
                    island.moveBiomass(b, cell, target);
                }
            }
        }
    }

    private Cell selectTargetCell(Cell cell, int speed) {
        int dx = ThreadLocalRandom.current().nextInt(-speed, speed + 1);
        int dy = ThreadLocalRandom.current().nextInt(-speed, speed + 1);
        return cell.getIsland().getCell(cell.getX() + dx, cell.getY() + dy);
    }
}
