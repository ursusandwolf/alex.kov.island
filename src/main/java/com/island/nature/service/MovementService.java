package com.island.nature.service;

import static com.island.nature.config.SimulationConstants.BIOMASS_MOVE_CHUNK_BP;
import static com.island.nature.config.SimulationConstants.ENDANGERED_SPEED_BONUS;
import static com.island.nature.config.SimulationConstants.SCALE_10K;
import static com.island.nature.config.SimulationConstants.SPEED_MOVE_COST_STEP_BP;

import com.island.engine.SimulationNode;
import com.island.nature.config.SimulationConstants;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.BiomassManager;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureRegistry;
import com.island.nature.entities.NatureStatistics;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.model.Cell;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal and mobile biomass movement using integer arithmetic.
 */
public class MovementService extends AbstractService {
    private final NatureRegistry registry;
    private final NatureStatistics statistics;
    private final BiomassManager biomassManager;

    public MovementService(NatureWorld world, SpeciesRegistry speciesRegistry, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.registry = world;
        this.statistics = world;
        this.biomassManager = world;
    }

    public MovementService(NatureRegistry registry, NatureStatistics statistics, BiomassManager biomassManager, 
                           ExecutorService executor, RandomProvider random) {
        super(null, executor, random);
        this.registry = registry;
        this.statistics = statistics;
        this.biomassManager = biomassManager;
    }

    @Override
    public void processCell(Cell cell, int tickCount) {
        processAnimals(cell, tickCount);
        processMobileBiomass(cell);
    }

    private void processAnimals(Cell node, int tickCount) {
        node.forEachAnimalSampled(SimulationConstants.MOVEMENT_LOD_LIMIT, getRandom(), animal -> {
            if (animal.isAlive()) {
                if (shouldAct(animal, AnimalType.Action.MOVE, tickCount)) {
                    int speed = animal.getSpeed();
                    if (protectionMap != null && protectionMap.containsKey(animal.getSpeciesKey())) {
                        speed += ENDANGERED_SPEED_BONUS;
                    }
                    
                    if (speed > 0) {
                        SimulationNode<Organism> target = selectTargetNode(node, speed);
                        if (target != node) {
                            if (getWorld().moveEntity(animal, node, target)) {
                                long moveCost = (animal.getMaxEnergy() * (1 + animal.getSpeed()) * SPEED_MOVE_COST_STEP_BP) / SCALE_10K;
                                animal.consumeEnergy(moveCost);
                                if (!animal.isAlive()) {
                                    statistics.reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private void processMobileBiomass(Cell node) {
        List<Biomass> mobile = new ArrayList<>();
        node.forEachEntity(e -> {
            if (e instanceof Biomass b && b.isAlive() && b.getSpeed() > 0 && b.getBiomass() > 0) {
                mobile.add(b);
            }
        });

        for (Biomass b : mobile) {
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
                if (target != node && target instanceof Cell targetCell) {
                    biomassManager.moveBiomassPartially(b, node, targetCell, chunk);
                }
            });
        }
    }

    private SimulationNode<Organism> selectTargetNode(Cell node, int speed) {
        if (speed == 1) {
            List<SimulationNode<Organism>> neighbors = node.getNeighbors();
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
