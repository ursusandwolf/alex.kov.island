package com.island.service;

import static com.island.config.SimulationConstants.BIOMASS_MOVE_CHUNK_FRACTION;
import static com.island.config.SimulationConstants.SPEED_MOVE_COST_STEP_PERCENT;
import static com.island.config.SimulationConstants.ENDANGERED_SPEED_BONUS;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal and mobile biomass movement.
 */
public class MovementService extends AbstractService<Cell> {
    private final SpeciesRegistry speciesRegistry;
    private Map<SpeciesKey, Double> protectionMap;

    public MovementService(SimulationWorld world, SpeciesRegistry speciesRegistry, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.speciesRegistry = speciesRegistry;
    }

    @Override
    public void tick(int tickCount) {
        this.protectionMap = getWorld().getProtectionMap(speciesRegistry);
        super.tick(tickCount);
    }

    @Override
    protected void processCell(Cell cell, int tickCount) {
        processAnimals(cell, tickCount);
        processMobileBiomass(cell);
    }

    private void processAnimals(Cell cell, int tickCount) {
        forEachSampled(cell.getAnimals(), 100, animal -> {
            if (animal.isAlive()) {
                if (shouldMove(animal, tickCount)) {
                    double moveCost = animal.getMaxEnergy() * (1 + animal.getSpeed()) * SPEED_MOVE_COST_STEP_PERCENT;
                    animal.consumeEnergy(moveCost);

                    if (!animal.isAlive()) {
                        getWorld().reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                    } else {
                        int speed = animal.getSpeed();
                        if (protectionMap != null && protectionMap.containsKey(animal.getSpeciesKey())) {
                            speed += ENDANGERED_SPEED_BONUS;
                        }
                        
                        if (speed > 0) {
                            SimulationNode target = selectTargetNode(cell, speed);
                            if (target != cell) {
                                getWorld().moveAnimal(animal, cell, target);
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean shouldMove(Animal animal, int tickCount) {
        if (!animal.canPerformAction()) {
            return false;
        }
        return (tickCount % animal.getAnimalType().getTickInterval(com.island.content.AnimalType.Action.MOVE) == 0);
    }

    private void processMobileBiomass(Cell cell) {
        List<Biomass> containers = cell.getBiomassContainers();
        for (Biomass b : containers) {
            if (b.isAlive() && b.getSpeed() > 0 && b.getBiomass() > 0) {
                double totalMass = b.getBiomass();
                double chunk = totalMass * BIOMASS_MOVE_CHUNK_FRACTION;

                int direction = getRandom().nextInt(4);
                int dx = 0;
                int dy = 0;
                switch (direction) {
                    case 0 -> dy = -1;
                    case 1 -> dy = 1;
                    case 2 -> dx = -1;
                    case 3 -> dx = 1;
                    default -> { }
                }

                getWorld().getNode(cell, dx, dy).ifPresent(target -> {
                    if (target != cell) {
                        getWorld().moveBiomassPartially(b, cell, target, chunk);
                    }
                });
            }
        }
    }

    private SimulationNode selectTargetNode(SimulationNode node, int speed) {
        if (speed == 1) {
            List<SimulationNode> neighbors = node.getNeighbors();
            if (!neighbors.isEmpty()) {
                int choice = getRandom().nextInt(neighbors.size() + 1);
                return (choice < neighbors.size()) ? neighbors.get(choice) : node;
            }
        }
        
        int dx = getRandom().nextInt(-speed, speed + 1);
        int dy = getRandom().nextInt(-speed, speed + 1);
        return getWorld().getNode(node, dx, dy).orElse(node);
    }
}
