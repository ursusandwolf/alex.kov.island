package com.island.view;

import com.island.content.Animal;
import com.island.content.Plant;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.HashMap;
import java.util.Map;

public class ConsoleView {
    private static final Map<String, String> ICONS = new HashMap<>();

    static {
        ICONS.put("wolf", "🐺"); ICONS.put("boa", "🐍"); ICONS.put("fox", "🦊");
        ICONS.put("bear", "🐻"); ICONS.put("eagle", "🦅"); ICONS.put("horse", "🐎");
        ICONS.put("deer", "🦌"); ICONS.put("rabbit", "🐇"); ICONS.put("mouse", "🐁");
        ICONS.put("goat", "🐐"); ICONS.put("sheep", "🐑"); ICONS.put("boar", "🐗");
        ICONS.put("buffalo", "🐃"); ICONS.put("duck", "🦆"); ICONS.put("caterpillar", "🐛");
        ICONS.put("plant", "🌿");
    }

    public void display(Island island) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("СОСТОЯНИЕ ОСТРОВА:");
        
        Map<String, Integer> counts = new HashMap<>();
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                cell.getAnimals().forEach(a -> { if (a.isAlive()) counts.put(a.getSpeciesKey(), counts.getOrDefault(a.getSpeciesKey(), 0) + 1); });
                cell.getPlants().forEach(p -> { if (p.isAlive()) counts.put(p.getSpeciesKey(), counts.getOrDefault(p.getSpeciesKey(), 0) + 1); });
            }
        }

        counts.forEach((key, count) -> System.out.printf("%s %-12s: %d\n", ICONS.getOrDefault(key, "❓"), key, count));
        System.out.println("-".repeat(50));
        System.out.printf("Всего организмов: %d\n", island.getTotalOrganismCount());
        System.out.println("=".repeat(50));
        
        System.out.println("Фрагмент карты (10x5):");
        for (int y = 0; y < Math.min(island.getHeight(), 5); y++) {
            for (int x = 0; x < Math.min(island.getWidth(), 10); x++) {
                Cell cell = island.getCell(x, y);
                if (cell.getAnimalCount() > 0) {
                    System.out.print(ICONS.getOrDefault(cell.getAnimals().get(0).getSpeciesKey(), "🐾") + " ");
                } else if (cell.getPlantCount() > 0) {
                    System.out.print(ICONS.getOrDefault("plant", "🌿") + " ");
                } else {
                    System.out.print(".  ");
                }
            }
            System.out.println();
        }
    }
}
