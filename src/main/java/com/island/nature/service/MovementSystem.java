package com.island.nature.service;

import com.island.engine.core.SimulationNode;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.EntityQuery;
import com.island.nature.entities.components.MovementComponent;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.registry.BiomassManager;
import com.island.nature.model.Cell;
import com.island.util.common.RandomProvider;
import com.island.util.sampling.SamplingContext;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * ECS System responsible for animal and mobile biomass movement.
 */
public class MovementSystem extends NatureEntitySystem {
    private final BiomassManager biomassManager;

    public MovementSystem(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.biomassManager = world;
    }

    @Override
    public List<Class<? extends Component>> requiredComponents() {
        return List.of(MovementComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_MOVEMENT;
    }

    @Override
    protected void doProcessCell(Cell cell, int tickCount) {
        // 1. Process Animals with sampling to maintain performance
        cell.forEachAnimalSampled(new SamplingContext(config.getMovementLodLimit(), getRandom()), animal -> {
            if (animal.getComponent(MovementComponent.class) != null) {
                processAnimal(animal, cell, tickCount);
            }
        });

        // 2. Process Mobile Biomass (they also have MovementComponent)
        cell.query(new EntityQuery<>(requiredComponents()), entity -> {
            if (entity instanceof Biomass biomass && biomass.getSpeed() > 0 && biomass.getBiomass() > 0) {
                processBiomass(biomass, cell);
            }
        });
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        // Not used directly because we override doProcessCell to handle sampling and Biomass specifically
    }

    private void processAnimal(Animal animal, Cell node, int tickCount) {
        if (!animal.isAlive()) return;

        if (shouldAct(animal, AnimalType.Action.MOVE, tickCount)) {
            MovementComponent move = animal.getComponent(MovementComponent.class);
            int speed = (move != null) ? move.getSpeed() : 0;
            
            if (protectionMap != null && protectionMap.containsKey(animal.getSpeciesKey())) {
                speed += config.getEndangeredSpeedBonus();
            }
            
            if (speed > 0) {
                SimulationNode<Organism> target = selectTargetNode(node, speed);
                if (target != node) {
                    if (getWorld().moveEntity(animal, node, target)) {
                        long moveCost = (animal.getMaxEnergy() * (1 + speed) * config.getSpeedMoveCostStepBP()) / config.getScale10K();
                        animal.consumeEnergy(moveCost);
                        if (!animal.isAlive()) {
                            animal.die(DeathCause.MOVEMENT_EXHAUSTION);
                        }
                    }
                }
            }
        }
    }

    private void processBiomass(Biomass b, Cell node) {
        long totalMass = b.getBiomass();
        long chunk = (totalMass * config.getBiomassMoveChunkBP()) / config.getScale10K();

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
