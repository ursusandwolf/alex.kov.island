package com.island.service;

import com.island.content.Animal;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.List;

public class LifecycleService implements Runnable {
    private final Island island;
    private static final double METABOLISM_RATE = 0.1; // 10% потери энергии за такт

    public LifecycleService(Island island) {
        this.island = island;
    }

    @Override
    public void run() {
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                processCell(cell);
                cell.cleanupDeadOrganisms();
            }
        }
    }

    private void processCell(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        for (Animal animal : animals) {
            if (animal.isAlive()) {
                // Метаболизм: животное теряет энергию просто живя
                // В Animal.java мы должны добавить доступ к методу consumeEnergy или аналогичному
                // Пока используем внутреннюю логику checkState
                animal.checkState();
            }
        }
    }
}
