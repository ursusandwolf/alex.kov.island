package com.island.service;

import static com.island.config.SimulationConstants.BIOMASS_MOVE_CHUNK_FRACTION;
import static com.island.config.SimulationConstants.ENDANGERED_POPULATION_THRESHOLD;
import static com.island.config.SimulationConstants.ENDANGERED_SPEED_BONUS;
import static com.island.config.SimulationConstants.SPEED_MOVE_COST_STEP_PERCENT;

import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal and mobile biomass movement.
 */
public class MovementService extends AbstractService {

    public MovementService(Island island, ExecutorService executor, RandomProvider random) {
        super(island, executor, random);
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

                double moveCost = animal.getMaxEnergy() * (1 + animal.getSpeed()) * SPEED_MOVE_COST_STEP_PERCENT;
                animal.consumeEnergy(moveCost);

                if (!animal.isAlive()) {
                    island.reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                    continue;
                }
                
                int speed = animal.getSpeed();
                
                // --- Red Book Mobility Bonus ---
                int currentCount = island.getSpeciesCount(animal.getSpeciesKey());
                int globalCapacity = islandArea * animal.getMaxPerCell();
                if (currentCount > 0 && currentCount < globalCapacity * ENDANGERED_POPULATION_THRESHOLD) {
                    speed += ENDANGERED_SPEED_BONUS; 
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
                // Logic: Move exactly one chunk in one random direction.
                double totalMass = b.getBiomass();
                double chunk = totalMass * BIOMASS_MOVE_CHUNK_FRACTION;

                int direction = getRandom().nextInt(4); // 0: Up, 1: Down, 2: Left, 3: Right
                int dx = 0;
                int dy = 0;
                switch (direction) {
                    case 0 -> dy = -1;
                    case 1 -> dy = 1;
                    case 2 -> dx = -1;
                    case 3 -> dx = 1;
                    default -> { /* Should not happen */ }
                }

                int tx = cell.getX() + dx;
                int ty = cell.getY() + dy;

                // "Если на краю карты показалось неправильное направление, то пропуск хода"
                if (tx >= 0 && tx < island.getWidth() && ty >= 0 && ty < island.getHeight()) {
                    Cell target = island.getCell(tx, ty);
                    if (target != cell) {
                        island.moveBiomassPartially(b, cell, target, chunk);
                    }
                }
            }
        }
    }

    private Cell selectTargetCell(Cell cell, int speed) {
        int dx = getRandom().nextInt(-speed, speed + 1);
        int dy = getRandom().nextInt(-speed, speed + 1);
        return cell.getIsland().getCell(cell.getX() + dx, cell.getY() + dy);
    }
}
