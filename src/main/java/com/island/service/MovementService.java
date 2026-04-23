package com.island.service;

import com.island.content.Animal;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MovementService implements Runnable {
    private final Island island;

    public MovementService(Island island) {
        this.island = island;
    }

    @Override
    public void run() {
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                processCell(island.getCell(x, y));
            }
        }
    }

    private void processCell(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        for (Animal animal : animals) {
            if (!animal.isAlive()) continue;
            // Простая логика перемещения
            int speed = animal.getSpeed();
            if (speed > 0) {
                int dx = ThreadLocalRandom.current().nextInt(-speed, speed + 1);
                int dy = ThreadLocalRandom.current().nextInt(-speed, speed + 1);
                int tx = cell.getX() + dx;
                int ty = cell.getY() + dy;
                
                Cell target = island.getCell(tx, ty); // Остров зациклен в Island.getCell
                if (target != cell && target.addAnimal(animal)) {
                    cell.removeAnimal(animal);
                }
            }
        }
    }
}
