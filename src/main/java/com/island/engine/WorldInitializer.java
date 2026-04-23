package com.island.engine;

import com.island.content.AnimalFactory;
import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.model.Cell;

import java.util.concurrent.ThreadLocalRandom;

public class WorldInitializer {
    private final AnimalFactory animalFactory;

    public WorldInitializer(AnimalFactory animalFactory) {
        this.animalFactory = animalFactory;
    }

    public void initialize(Island island, SpeciesConfig config) {
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                // Пример начального наполнения
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    // Логика спавна организмов
                }
            }
        }
    }
}
