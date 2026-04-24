package com.island.view;

import com.island.content.Animal;
import com.island.content.Plant;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.*;
import java.util.stream.Collectors;

public class ConsoleView {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static final Map<String, String> ICONS = new HashMap<>();
    private static final Map<String, Integer> DISPLAY_PRIORITY = new HashMap<>();

    static {
        ICONS.put("wolf", "🐺"); ICONS.put("boa", "🐍"); ICONS.put("fox", "🦊");
        ICONS.put("bear", "🐻"); ICONS.put("eagle", "🦅"); ICONS.put("horse", "🐎");
        ICONS.put("deer", "🦌"); ICONS.put("rabbit", "🐇"); ICONS.put("mouse", "🐁");
        ICONS.put("goat", "🐐"); ICONS.put("sheep", "🐑"); ICONS.put("boar", "🐗");
        ICONS.put("buffalo", "🐃"); ICONS.put("duck", "🦆"); ICONS.put("caterpillar", "🐛");
        ICONS.put("plant", "🌿");

        DISPLAY_PRIORITY.put("bear", 100);
        DISPLAY_PRIORITY.put("wolf", 90);
        DISPLAY_PRIORITY.put("boa", 80);
        DISPLAY_PRIORITY.put("eagle", 70);
        DISPLAY_PRIORITY.put("fox", 60);
        DISPLAY_PRIORITY.put("buffalo", 55);
        DISPLAY_PRIORITY.put("horse", 50);
        DISPLAY_PRIORITY.put("deer", 45);
        DISPLAY_PRIORITY.put("boar", 40);
        DISPLAY_PRIORITY.put("sheep", 35);
        DISPLAY_PRIORITY.put("goat", 30);
        DISPLAY_PRIORITY.put("rabbit", 25);
        DISPLAY_PRIORITY.put("duck", 20);
        DISPLAY_PRIORITY.put("mouse", 10);
        DISPLAY_PRIORITY.put("caterpillar", 5);
        DISPLAY_PRIORITY.put("plant", 1);
    }

    public void display(Island island) {
        StringBuilder sb = new StringBuilder();
        sb.append("\033[H"); 
        
        sb.append(CYAN).append("=== ISLAND ECOSYSTEM DASHBOARD ===").append(RESET).append("\n");
        sb.append(String.format("Tick: %-5d | Total Population: %-6d\n", 
                island.getTickCount(), island.getTotalOrganismCount()));
        sb.append("-".repeat(60)).append("\n");

        Map<String, Integer> counts = new TreeMap<>();
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                cell.getAnimals().stream().filter(Animal::isAlive).forEach(a -> 
                    counts.put(a.getSpeciesKey(), counts.getOrDefault(a.getSpeciesKey(), 0) + 1));
                cell.getPlants().stream().filter(Plant::isAlive).forEach(p -> 
                    counts.put(p.getSpeciesKey(), counts.getOrDefault(p.getSpeciesKey(), 0) + 1));
            }
        }

        int col = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            sb.append(String.format("%s %-11s: %-5d  ", ICONS.get(entry.getKey()), entry.getKey(), entry.getValue()));
            if (++col % 4 == 0) sb.append("\n");
        }
        if (col % 4 != 0) sb.append("\n");

        sb.append("-".repeat(60)).append("\n");
        sb.append(YELLOW).append("Map View (8x8) [2x2 icons per cell]:").append(RESET).append("\n");
        
        int midX = island.getWidth() / 2;
        int midY = island.getHeight() / 2;

        for (int y = 0; y < island.getHeight(); y++) {
            if (y > 0 && y == midY) {
                sb.append("=".repeat(island.getWidth() * 7 + 2)).append("\n");
            }
            
            StringBuilder topLine = new StringBuilder();
            StringBuilder bottomLine = new StringBuilder();

            for (int x = 0; x < island.getWidth(); x++) {
                if (x > 0 && x == midX) {
                    topLine.append("║ ");
                    bottomLine.append("║ ");
                }
                
                String[] icons = getCellIcons(island.getCell(x, y));
                topLine.append("[").append(icons[0]).append(icons[1]).append("] ");
                bottomLine.append("[").append(icons[2]).append(icons[3]).append("] ");
            }
            sb.append(topLine).append("\n").append(bottomLine).append("\n");
        }
        
        System.out.print(sb.toString());
        System.out.flush();
    }

    private String[] getCellIcons(Cell cell) {
        Set<String> species = cell.getAnimals().stream()
                .filter(Animal::isAlive)
                .map(Animal::getSpeciesKey)
                .collect(Collectors.toSet());

        List<String> sorted = new ArrayList<>(species);
        sorted.sort((s1, s2) -> DISPLAY_PRIORITY.getOrDefault(s2, 0) - DISPLAY_PRIORITY.getOrDefault(s1, 0));

        String[] result = new String[4];
        int iconsFound = 0;
        
        for (int i = 0; i < Math.min(sorted.size(), 4); i++) {
            result[iconsFound++] = ICONS.get(sorted.get(i));
        }

        if (iconsFound < 4 && cell.getPlants().stream().anyMatch(Plant::isAlive)) {
            result[iconsFound++] = GREEN + ICONS.get("plant") + RESET;
        }

        while (iconsFound < 4) {
            result[iconsFound++] = ". ";
        }
        
        // Ensure all strings are 2-char wide (emojis are usually 2-char wide in terminal representation)
        // But in Java string they are often 1 or 2 surrogates. 
        // Most emojis like 🐺 are 1 char in length but 2 columns wide.
        return result;
    }
}
