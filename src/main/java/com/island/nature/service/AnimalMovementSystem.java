package com.island.nature.service;

import com.island.engine.core.SimulationNode;
import com.island.engine.ecs.Component;
import com.island.nature.entities.components.MetabolismComponent;
import com.island.nature.entities.components.MovementComponent;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.model.Cell;
import com.island.util.common.RandomProvider;
import com.island.util.sampling.SamplingContext;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * ECS System responsible for animal movement with performance sampling.
 */
public class AnimalMovementSystem extends NatureEntitySystem {

    public AnimalMovementSystem(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    public List<Class<? extends Component>> requiredComponents() {
        return List.of(MovementComponent.class, MetabolismComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_MOVEMENT;
    }

    @Override
    protected void doProcessCell(Cell cell, int tickCount) {
        // Animals use sampling to maintain performance
        cell.forEachAnimalSampled(new SamplingContext(config.getMovementLodLimit(), getRandom()), animal -> {
            // Re-verify components due to sampling potentially bypassing query filter if used directly
            if (animal.getComponent(MovementComponent.class) != null && animal.getComponent(MetabolismComponent.class) != null) {
                process(animal, cell, tickCount);
            }
        });
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        Animal animal = (Animal) entity;
        if (!animal.isAlive()) {
            return;
        }

        if (shouldAct(animal, AnimalType.Action.MOVE, tickCount)) {
            MovementComponent move = animal.getComponent(MovementComponent.class);
            int speed = (move != null) ? move.getSpeed() : 0;
            
            if (protectionMap != null && protectionMap.containsKey(animal.getSpeciesKey())) {
                speed += config.getEndangeredSpeedBonus();
            }
            
            if (speed > 0) {
                SimulationNode<Organism> target = selectTargetNode(cell, speed);
                if (target != cell) {
                    if (getWorld().moveEntity(animal, cell, target)) {
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
