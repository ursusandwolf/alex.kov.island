package com.island.nature.view;

import com.island.engine.NodeSnapshot;
import com.island.engine.WorldSnapshot;
import com.island.util.ViewUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Console visualization of the simulation.
 * Domain-agnostic: relies on WorldSnapshot and species codes.
 */
public class ConsoleView implements SimulationView {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String CLEAR_EOL = "\u001B[K";
    private static final String CURSOR_HOME = "\u001B[H";
    private static final String CLEAR_DOWN = "\u001B[J";

    private static final Map<String, String> ICONS = new HashMap<>();

    static {
        ICONS.put("wolf", "🐺");
        ICONS.put("boa", "🐍");
        ICONS.put("fox", "🦊");
        ICONS.put("bear", "🐻");
        ICONS.put("eagle", "🦅");
        ICONS.put("horse", "🐎");
        ICONS.put("deer", "🦌");
        ICONS.put("rabbit", "🐇");
        ICONS.put("mouse", "🐁");
        ICONS.put("hamster", "🐹");
        ICONS.put("goat", "🐐");
        ICONS.put("sheep", "🐑");
        ICONS.put("boar", "🐗");
        ICONS.put("buffalo", "🐃");
        ICONS.put("duck", "🦆");
        ICONS.put("frog", "🐸");
        ICONS.put("chameleon", "🦎");
        ICONS.put("caterpillar", "🐛");
        ICONS.put("butterfly", "🦋");
        ICONS.put("plant", "🌿");
        ICONS.put("grass", "🌿");
        ICONS.put("mushroom", "🍄");
    }

    private boolean isSilent = false;
    private final LinkedList<Integer> totalPopulationHistory = new LinkedList<>();
    private final Map<String, LinkedList<Integer>> populationHistory = new HashMap<>();
    private static final int HISTORY_SIZE = 10;
    
    @Override
    public void setSilent(boolean silent) {
        this.isSilent = silent;
    }

    @Override
    public void display(WorldSnapshot snapshot) {
        if (isSilent) {
            return;
        }

        updateHistory(snapshot);
        
        StringBuilder sb = new StringBuilder();
        sb.append(CURSOR_HOME); 
        sb.append(CYAN).append("=== SIMULATION DASHBOARD ===").append(RESET).append(CLEAR_EOL).append("\n");
        
        String totalGraph = ViewUtils.getSparkline(totalPopulationHistory, HISTORY_SIZE * 2);
        sb.append(String.format("Tick: %d | Total Entities: %d %s", 
                snapshot.getTickCount(), snapshot.getTotalEntityCount(), totalGraph)).append(CLEAR_EOL).append("\n");
        
        // Hunger Stats
        Map<String, Number> metrics = snapshot.getMetrics();
        double satiety = metrics.getOrDefault("globalSatiety", 0.0).doubleValue();
        int hungry = metrics.getOrDefault("hungryCount", 0).intValue();
        String satietyColor = satiety > 70 ? GREEN : (satiety > 40 ? YELLOW : "\u001B[31m");

        sb.append(String.format("Global Satiety: %s%3.1f%%%s [", satietyColor, satiety, RESET));
        int progress = (int) Math.max(0, Math.min(20, satiety / 5));
        sb.append(satietyColor).append("#".repeat(progress)).append(".".repeat(20 - progress)).append(RESET).append("] ");
        sb.append(String.format("| Hungry: %s%d%s", (hungry > 0 ? "\u001B[31m" : GREEN), hungry, RESET)).append(CLEAR_EOL).append("\n");        
        int hungerTotal = metrics.getOrDefault("deaths.HUNGER", 0).intValue();
        int ageTotal = metrics.getOrDefault("deaths.AGE", 0).intValue();
        int eatenTotal = metrics.getOrDefault("deaths.EATEN", 0).intValue() + 
                         metrics.getOrDefault("deaths.EATEN_BY_PACK", 0).intValue();
        int exhaustTotal = metrics.getOrDefault("deaths.MOVEMENT_EXHAUSTION", 0).intValue() +
                           metrics.getOrDefault("deaths.REPRODUCTION_EXHAUSTION", 0).intValue();

        sb.append(String.format("Total Deaths: Hunger: %s%d%s | Old Age: %s%d%s | Exhausted: %s%d%s", 
                "\u001B[31m", hungerTotal, RESET, 
                YELLOW, ageTotal, RESET,
                "\u001B[35m", exhaustTotal, RESET)).append(CLEAR_EOL).append("\n"); 
        sb.append(String.format("Total Hunts (Eaten Animals): %s%d%s", 
                GREEN, eatenTotal, RESET)).append(CLEAR_EOL).append("\n");

        sb.append("-".repeat(60)).append(CLEAR_EOL).append("\n");

        Map<String, Integer> currentCounts = new TreeMap<>();
        metrics.forEach((k, v) -> {
            if (k.startsWith("species.")) {
                currentCounts.put(k.substring(8), v.intValue());
            }
        });

        renderStatsWithGraphs(sb, currentCounts);

        sb.append("-".repeat(60)).append(CLEAR_EOL).append("\n");
        sb.append(YELLOW).append("Map View:").append(RESET).append(CLEAR_EOL).append("\n");
        
        sb.append("╔").append("═══".repeat(snapshot.getWidth())).append("╗").append(CLEAR_EOL).append("\n");
        for (int y = 0; y < snapshot.getHeight(); y++) {
            sb.append("║");
            for (int x = 0; x < snapshot.getWidth(); x++) {
                renderNode(sb, snapshot.getNodeSnapshot(x, y));
            }
            sb.append("║").append(CLEAR_EOL).append("\n");
        }
        sb.append("╚").append("═══".repeat(snapshot.getWidth())).append("╝").append(CLEAR_EOL).append("\n");
        sb.append(CLEAR_DOWN); 
        
        System.out.print(sb.toString());
        System.out.flush();
    }

    private void updateHistory(WorldSnapshot snapshot) {
        totalPopulationHistory.add(snapshot.getTotalEntityCount());
        if (totalPopulationHistory.size() > HISTORY_SIZE * 2) {
            totalPopulationHistory.removeFirst();
        }

        Map<String, Number> metrics = snapshot.getMetrics();
        metrics.forEach((k, v) -> {
            if (k.startsWith("species.")) {
                String speciesCode = k.substring(8);
                populationHistory.putIfAbsent(speciesCode, new LinkedList<>());
                LinkedList<Integer> history = populationHistory.get(speciesCode);
                history.add(v.intValue());
                if (history.size() > HISTORY_SIZE) {
                    history.removeFirst();
                }
            }
        });
    }

    private void renderStatsWithGraphs(StringBuilder sb, Map<String, Integer> currentCounts) {
        int col = 0;
        List<String> allSpecies = new ArrayList<>(currentCounts.keySet());
        Collections.sort(allSpecies);

        for (String speciesCode : allSpecies) {
            int count = currentCounts.getOrDefault(speciesCode, 0);
            String icon = ICONS.getOrDefault(speciesCode, "?");
            String graph = ViewUtils.getSparkline(populationHistory.getOrDefault(speciesCode, new LinkedList<>()), HISTORY_SIZE);
            
            sb.append(String.format("%s %-11s: %-5d %s  ", icon, speciesCode, count, graph));
            if (++col % 2 == 0) {
                sb.append(CLEAR_EOL).append("\n");
            }
        }
        if (col % 2 != 0) {
            sb.append(CLEAR_EOL).append("\n");
        }
    }

    private void renderNode(StringBuilder sb, NodeSnapshot node) {
        if (node.hasOrganisms()) {
            String icon = ICONS.getOrDefault(node.getTopSpeciesCode(), "?");
            if (node.isTopSpeciesPlant()) {
                sb.append(GREEN).append(icon).append(RESET).append(" ");
            } else {
                sb.append(icon).append(" ");
            }
        } else {
            sb.append("🏜️ ");
        }
    }
}