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
        List<Animal> animals = cell.getAnimals();
        for (Animal animal : animals) {
            if (!animal.isAlive()) continue;
            
            int speed = animal.getSpeed();
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
