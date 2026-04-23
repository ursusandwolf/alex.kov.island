package com.island.engine;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesConfig;
import com.island.content.Plant;
import com.island.model.Island;
import com.island.model.Cell;

import java.util.concurrent.ThreadLocalRandom;

public class WorldInitializer {

    public void initialize(Island island, SpeciesConfig config) {
        // Заполняем мир животными и растениями
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                
                // Рандомно заселяем волками
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    Animal wolf = AnimalFactory.createAnimal("wolf");
                    if (wolf != null) cell.addAnimal(wolf);
                }
                
                // Рандомно заселяем кроликами
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    Animal rabbit = AnimalFactory.createAnimal("rabbit");
                    if (rabbit != null) cell.addAnimal(rabbit);
                }

                // Рандомно заселяем лисами
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    Animal fox = AnimalFactory.createAnimal("fox");
                    if (fox != null) cell.addAnimal(fox);
                }

                // Растения
                if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                    // Используем анонимный класс для примера, если нет Grass.java
                    // Но лучше создать конкретный класс.
                    cell.addPlant(new Plant(1.0, 0.1, 0) {
                        @Override
                        public String getTypeName() { return "Grass"; }
                        @Override
                        public String getSpeciesKey() { return "Plant"; }
                        @Override
                        public Plant reproduce() {
                            return (ThreadLocalRandom.current().nextDouble() < 0.2) ? 
                                new Plant(maxBiomass, growthRate, 0) {
                                    @Override public String getTypeName() { return "Grass"; }
                                    @Override public String getSpeciesKey() { return "Plant"; }
                                    @Override public Plant reproduce() { return super.reproduce(); }
                                } : null;
                        }
                    });
                }
            }
        }
    }
}
