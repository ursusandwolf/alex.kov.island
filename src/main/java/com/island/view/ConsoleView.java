package com.island.view;

import com.island.content.Animal;
import com.island.content.Plant;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ConsoleView {
    // ANSI codes for UI
    private static final String RESET = "\u001B[0m";
    private static final String CLEAR_SCREEN = "\033[H\033[2J";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

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
        StringBuilder sb = new StringBuilder();
        
        // Reset cursor to top-left instead of full clear to prevent flicker
        sb.append("\033[H"); 
        
        sb.append(CYAN).append("=== ISLAND ECOSYSTEM DASHBOARD ===").append(RESET).append("\n");
        sb.append(String.format("Tick: %-5d | Total Population: %-6d\n", 
                island.getTickCount(), island.getTotalOrganismCount()));
        sb.append("-".repeat(40)).append("\n");

        Map<String, Integer> counts = new TreeMap<>(); // Sorted for stable UI
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                cell.getAnimals().forEach(a -> { 
                    if (a.isAlive()) counts.put(a.getSpeciesKey(), counts.getOrDefault(a.getSpeciesKey(), 0) + 1); 
                });
                cell.getPlants().forEach(p -> { 
                    if (p.isAlive()) counts.put(p.getSpeciesKey(), counts.getOrDefault(p.getSpeciesKey(), 0) + 1); 
                });
            }
        }

        // Print stats in 3 columns to save vertical space
        int col = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String icon = ICONS.getOrDefault(entry.getKey(), "🐾");
            sb.append(String.format("%s %-11s: %-5d  ", icon, entry.getKey(), entry.getValue()));
            if (++col % 3 == 0) sb.append("\n");
        }
        if (col % 3 != 0) sb.append("\n");

        sb.append("-".repeat(40)).append("\n");
        sb.append(YELLOW).append("Live Map View (15x8):").append(RESET).append("\n");
        
        for (int y = 0; y < Math.min(island.getHeight(), 8); y++) {
            for (int x = 0; x < Math.min(island.getWidth(), 15); x++) {
                Cell cell = island.getCell(x, y);
                if (cell.getAnimalCount() > 0) {
                    Animal top = cell.getAnimals().get(0);
                    sb.append(ICONS.getOrDefault(top.getSpeciesKey(), "🐾")).append(" ");
                } else if (cell.getPlantCount() > 0) {
                    sb.append(GREEN).append(ICONS.get("plant")).append(RESET).append(" ");
                } else {
                    sb.append(".  ");
                }
            }
            sb.append("\n");
        }
        
        System.out.print(sb.toString());
        System.out.flush();
    }
}
