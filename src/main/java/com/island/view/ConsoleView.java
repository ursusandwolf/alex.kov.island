package com.island.view;
import com.island.util.RandomUtils;
import com.island.util.ViewUtils;import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.plants.Plant;
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
        ICONS.put("plant", "🌿"); ICONS.put("cabbage", "🥬");
    }

    private int lastRenderedTick = -1;
    private static final int RENDER_THROTTLE = 5;
    private boolean silent = false;

    private final Map<String, LinkedList<Integer>> populationHistory = new HashMap<>();
    private static final int HISTORY_SIZE = 15;

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public void display(Island island) {
        if (silent) return;
        
        // Update history every render tick
        if (island.getTickCount() % RENDER_THROTTLE == 0 || lastRenderedTick == -1) {
            updateHistory(island);
        }

        // Only update UI every RENDER_THROTTLE ticks to reduce flicker
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

        Map<String, Integer> currentCounts = new TreeMap<>(island.getSpeciesCounts());

        renderStatsWithGraphs(sb, currentCounts);

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

    private void updateHistory(Island island) {
        Map<String, Integer> currentCounts = island.getSpeciesCounts();
        for (String species : ICONS.keySet()) {
            populationHistory.putIfAbsent(species, new LinkedList<>());
            LinkedList<Integer> history = populationHistory.get(species);
            history.add(currentCounts.getOrDefault(species, 0));
            if (history.size() > HISTORY_SIZE) {
                history.removeFirst();
            }
        }
    }

    private void renderStatsWithGraphs(StringBuilder sb, Map<String, Integer> currentCounts) {
        int col = 0;
        for (Map.Entry<String, Integer> entry : currentCounts.entrySet()) {
            String species = entry.getKey();
            int count = entry.getValue();
            String graph = ViewUtils.getSparkline(populationHistory.get(species), HISTORY_SIZE);
            sb.append(String.format("%s %-11s: %-5d %s  ", ICONS.get(species), species, count, graph));
            if (++col % 2 == 0) sb.append("\n");
        }
        if (col % 2 != 0) sb.append("\n");
    }

    private String getSparkline(String species) {
        LinkedList<Integer> history = populationHistory.get(species);
        if (history == null || history.size() < 2) return "      ";
        
        int min = history.stream().min(Integer::compare).orElse(0);
        int max = history.stream().max(Integer::compare).orElse(1);
        int range = Math.max(1, max - min);

        char[] sparkChars = {' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
        StringBuilder sparkline = new StringBuilder();
        for (int val : history) {
            int idx = (int) (((double) (val - min) / range) * (sparkChars.length - 1));
            sparkline.append(sparkChars[idx]);
        }
        // Pad to consistent width
        while (sparkline.length() < HISTORY_SIZE) sparkline.insert(0, " ");
        return sparkline.toString();
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
        for (Plant p : cell.getPlants()) {
            if (p.isAlive()) {
                biomassMap.put(p.getSpeciesKey(), 
                    biomassMap.getOrDefault(p.getSpeciesKey(), 0.0) + p.getBiomass());
            }
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
            
            if (speciesToDisplay.equals("plant") || speciesToDisplay.equals("cabbage")) {
                sb.append(GREEN).append(ICONS.get(speciesToDisplay)).append(RESET).append(" ");
            } else {
                sb.append(ICONS.getOrDefault(speciesToDisplay, "🐾")).append(" ");
            }
        } else {
            sb.append(". ");
        }
    }
}
