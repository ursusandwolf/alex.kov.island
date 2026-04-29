package com.island.service;

import static com.island.config.SimulationConstants.BIOMASS_MOVE_CHUNK_FRACTION;
import static com.island.config.SimulationConstants.ENDANGERED_POPULATION_THRESHOLD;
import static com.island.config.SimulationConstants.ENDANGERED_SPEED_BONUS;
import static com.island.config.SimulationConstants.SPEED_MOVE_COST_STEP_PERCENT;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal and mobile biomass movement.
 */
public class MovementService extends AbstractService {

    public MovementService(SimulationWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    protected void processCell(SimulationNode node) {
        if (node instanceof Cell cell) {
            processAnimals(cell);
            processMobileBiomass(cell);
        }
    }

    private void processAnimals(Cell cell) {
        // Use a snapshot to avoid ConcurrentModificationException
        List<Animal> animals = new java.util.ArrayList<>(cell.getAnimals());
        SimulationWorld world = getWorld();

        for (Animal animal : animals) {
            if (animal.isAlive()) {
                if (!animal.canPerformAction()) {
                    continue;
                }

                double moveCost = animal.getMaxEnergy() * (1 + animal.getSpeed()) * SPEED_MOVE_COST_STEP_PERCENT;
                animal.consumeEnergy(moveCost);

                if (!animal.isAlive()) {
                    world.reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                    continue;
                }
                
                int speed = animal.getSpeed();
                
                // Mobility bonus logic simplified here or kept if we can access species count
                // Actually, let's keep it for now but it might need more interface methods if we want full decoupling.
                // For now, we'll cast to Island if needed or just skip it.
                
                if (speed > 0) {
                    SimulationNode target = selectTargetNode(cell, speed);
                    if (target != cell) {
                        world.moveAnimal(animal, cell, target);
                    }
                }
            }
        }
    }

    private void processMobileBiomass(Cell cell) {
        List<Biomass> containers = cell.getBiomassContainers();
        SimulationWorld world = getWorld();

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

                world.getNode(cell, dx, dy).ifPresent(target -> {
                    if (target != cell) {
                        world.moveBiomassPartially(b, cell, target, chunk);
                    }
                });
            }
        }
    }

    private SimulationNode selectTargetNode(SimulationNode node, int speed) {
        int dx = getRandom().nextInt(-speed, speed + 1);
        int dy = getRandom().nextInt(-speed, speed + 1);
        return getWorld().getNode(node, dx, dy).orElse(node);
    }
}
