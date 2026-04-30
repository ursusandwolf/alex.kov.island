package com.island.service;

import static com.island.config.SimulationConstants.BIOMASS_MOVE_CHUNK_BP;
import static com.island.config.SimulationConstants.SPEED_MOVE_COST_STEP_BP;
import static com.island.config.SimulationConstants.ENDANGERED_SPEED_BONUS;
import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.island.content.AnimalType;

/**
 * Service responsible for animal and mobile biomass movement using integer arithmetic.
 */
public class MovementService extends AbstractService<SimulationNode> {
    private final SpeciesRegistry speciesRegistry;

    public MovementService(SimulationWorld world, SpeciesRegistry speciesRegistry, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.speciesRegistry = speciesRegistry;
    }

    @Override
    public void processCell(SimulationNode node, int tickCount) {
        processAnimals(node, tickCount);
        processMobileBiomass(node);
    }

    private void processAnimals(SimulationNode node, int tickCount) {
        node.forEachAnimalSampled(com.island.config.SimulationConstants.MOVEMENT_LOD_LIMIT, getRandom(), animal -> {
            if (animal.isAlive()) {
                if (shouldAct(animal, AnimalType.Action.MOVE, tickCount)) {
                    int speed = animal.getSpeed();
                    if (protectionMap != null && protectionMap.containsKey(animal.getSpeciesKey())) {
                        speed += ENDANGERED_SPEED_BONUS;
                    }
                    
                    if (speed > 0) {
                        SimulationNode target = selectTargetNode(node, speed);
                        if (target != node) {
                            if (getWorld().moveAnimal(animal, node, target)) {
                                long moveCost = (animal.getMaxEnergy() * (1 + animal.getSpeed()) * SPEED_MOVE_COST_STEP_BP) / SCALE_10K;
                                animal.consumeEnergy(moveCost);
                                if (!animal.isAlive()) {
                                    getWorld().reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private void processMobileBiomass(SimulationNode node) {
        List<? extends com.island.engine.Mortal> containers = node.getBiomassEntities();
        for (com.island.engine.Mortal m : containers) {
            if (m instanceof Biomass b && b.isAlive() && b.getSpeed() > 0 && b.getBiomass() > 0) {
                long totalMass = b.getBiomass();
                long chunk = (totalMass * BIOMASS_MOVE_CHUNK_BP) / SCALE_10K;

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

                getWorld().getNode(node, dx, dy).ifPresent(target -> {
                    if (target != node) {
                        getWorld().moveBiomassPartially(b, node, target, chunk);
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
