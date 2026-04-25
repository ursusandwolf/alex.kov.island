package com.island.view;

import com.island.content.Animal;
import com.island.content.Plant;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.*;

public class ConsoleView {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
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

    private int lastRenderedTick = -1;
    private static final int RENDER_THROTTLE = 5;

    public void display(Island island) {
        // Only update UI every RENDER_THROTTLE ticks to reduce flicker and make events more visible
        if (island.getTickCount() > 0 && island.getTickCount() % RENDER_THROTTLE != 0 && island.getTickCount() != lastRenderedTick + 1) {
            if (lastRenderedTick != -1) return; 
        }
        lastRenderedTick = island.getTickCount();

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
        sb.append(YELLOW).append("Map View:").append(RESET).append("\n");
        
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
        // Calculate total biomass per species in this cell
        Map<String, Double> biomassMap = new HashMap<>();
        
        for (Animal a : cell.getAnimals()) {
            if (a.isAlive()) {
                biomassMap.put(a.getSpeciesKey(), 
                    biomassMap.getOrDefault(a.getSpeciesKey(), 0.0) + a.getWeight());
            }
        }

        // Add plants to biomass if applicable
        long alivePlants = cell.getPlants().stream().filter(Plant::isAlive).count();
        if (alivePlants > 0) {
            biomassMap.put("plant", alivePlants * 1.0); // 1kg per plant
        }

        if (!biomassMap.isEmpty()) {
            // Get top 3 species by biomass
            List<String> topSpeciesList = biomassMap.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
            
            // Cycle through the top species based on the current tick
            int displayIndex = (lastRenderedTick / RENDER_THROTTLE) % topSpeciesList.size();
            String speciesToDisplay = topSpeciesList.get(displayIndex);
            
            if (speciesToDisplay.equals("plant")) {
                sb.append(GREEN).append(ICONS.get("plant")).append(RESET).append(" ");
            } else {
                sb.append(ICONS.getOrDefault(speciesToDisplay, "🐾")).append(" ");
            }
        } else {
            sb.append(". ");
        }
    }
}
