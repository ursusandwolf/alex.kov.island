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
        sb.append("-".repeat(60)).append("\n");

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

        // Print stats in 4 columns to save vertical space
        int col = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String icon = ICONS.getOrDefault(entry.getKey(), "🐾");
            sb.append(String.format("%s %-11s: %-5d  ", icon, entry.getKey(), entry.getValue()));
            if (++col % 4 == 0) sb.append("\n");
        }
        if (col % 4 != 0) sb.append("\n");

        sb.append("-".repeat(60)).append("\n");
        sb.append(YELLOW).append("Map View (").append(island.getWidth()).append("x").append(island.getHeight()).append("):").append(RESET).append("\n");
        
        int midX = island.getWidth() / 2;
        int midY = island.getHeight() / 2;

        for (int y = 0; y < island.getHeight(); y++) {
            if (y > 0 && y == midY) {
                sb.append("---".repeat(island.getWidth() + 1)).append("\n");
            }
            for (int x = 0; x < island.getWidth(); x++) {
                if (x > 0 && x == midX) {
                    sb.append("| ");
                }
                renderCell(sb, island.getCell(x, y));
            }
            sb.append("\n");
        }
        
        System.out.print(sb.toString());
        System.out.flush();
    }

    private void renderCell(StringBuilder sb, Cell cell) {
        Map<String, Integer> cellCounts = new HashMap<>();
        cell.getAnimals().stream()
                .filter(com.island.content.Organism::isAlive)
                .forEach(a -> cellCounts.put(a.getSpeciesKey(), cellCounts.getOrDefault(a.getSpeciesKey(), 0) + 1));

        if (!cellCounts.isEmpty()) {
            Map.Entry<String, Integer> maxEntry = cellCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow();
            sb.append(ICONS.getOrDefault(maxEntry.getKey(), "🐾")).append("  ");
        } else {
            boolean hasPlants = cell.getPlants().stream().anyMatch(com.island.content.Organism::isAlive);
            if (hasPlants) {
                sb.append(GREEN).append(ICONS.get("plant")).append(RESET).append("  ");
            } else {
                sb.append(".   ");
            }
        }
    }
}
