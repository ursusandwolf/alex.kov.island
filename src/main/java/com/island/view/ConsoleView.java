package com.island.view;

import com.island.content.Animal;
import com.island.content.SpeciesKey;
import com.island.content.Biomass;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.ViewUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Console visualization of the island ecosystem.
 */
public class ConsoleView implements SimulationView {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String CLEAR_EOL = "\u001B[K";
    private static final String CURSOR_HOME = "\u001B[H";
    private static final String CLEAR_DOWN = "\u001B[J";

    private static final Map<SpeciesKey, String> ICONS = new HashMap<>();

    static {
        ICONS.put(SpeciesKey.WOLF, "🐺");
        ICONS.put(SpeciesKey.BOA, "🐍");
        ICONS.put(SpeciesKey.FOX, "🦊");
        ICONS.put(SpeciesKey.BEAR, "🐻");
        ICONS.put(SpeciesKey.EAGLE, "🦅");
        ICONS.put(SpeciesKey.HORSE, "🐎");
        ICONS.put(SpeciesKey.DEER, "🦌");
        ICONS.put(SpeciesKey.RABBIT, "🐇");
        ICONS.put(SpeciesKey.MOUSE, "🐁");
        ICONS.put(SpeciesKey.HAMSTER, "🐹");
        ICONS.put(SpeciesKey.GOAT, "🐐");
        ICONS.put(SpeciesKey.SHEEP, "🐑");
        ICONS.put(SpeciesKey.BOAR, "🐗");
        ICONS.put(SpeciesKey.BUFFALO, "🐃");
        ICONS.put(SpeciesKey.DUCK, "🦆");
        ICONS.put(SpeciesKey.FROG, "🐸");
        ICONS.put(SpeciesKey.CHAMELEON, "🦎");
        ICONS.put(SpeciesKey.CATERPILLAR, "🐛");
        ICONS.put(SpeciesKey.BUTTERFLY, "🦋");
        ICONS.put(SpeciesKey.PLANT, "🌿");
        ICONS.put(SpeciesKey.GRASS, "☘️");
        ICONS.put(SpeciesKey.CABBAGE, "🥬");
    }

    private boolean isSilent = false;
    private final LinkedList<Integer> totalPopulationHistory = new LinkedList<>();
    private final Map<SpeciesKey, LinkedList<Integer>> populationHistory = new HashMap<>();
    private static final int HISTORY_SIZE = 10;
    
    private final Map<SpeciesKey, Double> cellBiomassMap = new HashMap<>();

    public void setSilent(boolean silent) {
        this.isSilent = silent;
    }

    public void display(Island island) {
        if (isSilent) {
            return;
        }

        updateHistory(island);
        
        StringBuilder sb = new StringBuilder();
        sb.append(CURSOR_HOME); // Move cursor to top-left instead of clearing the screen to prevent flickering
        sb.append(CYAN).append("=== ISLAND ECOSYSTEM DASHBOARD ===").append(RESET).append(CLEAR_EOL).append("\n");
        
        String totalGraph = ViewUtils.getSparkline(totalPopulationHistory, HISTORY_SIZE * 2);
        sb.append(String.format("Tick: %d | Total Organisms: %d %s", 
                island.getTickCount(), island.getTotalOrganismCount(), totalGraph)).append(CLEAR_EOL).append("\n");
        
        // Hunger Stats
        double satiety = island.getGlobalSatiety();
        int starving = island.getStarvingCount();
        String satietyColor = satiety > 70 ? GREEN : (satiety > 40 ? YELLOW : "\u001B[31m"); 
        
        sb.append(String.format("Global Satiety: %s%3.1f%%%s [", satietyColor, satiety, RESET));
        int progress = (int) (satiety / 5);
        sb.append(satietyColor).append("#".repeat(progress)).append(".".repeat(20 - progress)).append(RESET).append("] ");
        sb.append(String.format("| Starving: %s%d%s", (starving > 0 ? "\u001B[31m" : GREEN), starving, RESET)).append(CLEAR_EOL).append("\n");
        
        int hungerTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.HUNGER);
        int ageTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.AGE);
        int eatenTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.EATEN);
        int exhaustTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.MOVEMENT_EXHAUSTION);

        sb.append(String.format("Total Deaths: Hunger: %s%d%s | Old Age: %s%d%s | Exhausted: %s%d%s", 
                "\u001B[31m", hungerTotal, RESET, 
                YELLOW, ageTotal, RESET,
                "\u001B[35m", exhaustTotal, RESET)).append(CLEAR_EOL).append("\n"); 
        sb.append(String.format("Total Hunts (Eaten Animals): %s%d%s", 
                GREEN, eatenTotal, RESET)).append(CLEAR_EOL).append("\n");

        sb.append("-".repeat(60)).append(CLEAR_EOL).append("\n");

        Map<SpeciesKey, Integer> currentCounts = new TreeMap<>(island.getSpeciesCounts());

        renderStatsWithGraphs(sb, currentCounts);

        sb.append("-".repeat(60)).append(CLEAR_EOL).append("\n");
        sb.append(YELLOW).append("Map View:").append(RESET).append(CLEAR_EOL).append("\n");
        
        sb.append("╔").append("═══".repeat(island.getWidth())).append("╗").append(CLEAR_EOL).append("\n");
        for (int y = 0; y < island.getHeight(); y++) {
            sb.append("║");
            for (int x = 0; x < island.getWidth(); x++) {
                renderCell(sb, island.getGrid()[x][y]);
            }
            sb.append("║").append(CLEAR_EOL).append("\n");
        }
        sb.append("╚").append("═══".repeat(island.getWidth())).append("╝").append(CLEAR_EOL).append("\n");
        sb.append(CLEAR_DOWN); 
        
        System.out.print(sb.toString());
        System.out.flush();
    }

    private void updateHistory(Island island) {
        totalPopulationHistory.add(island.getTotalOrganismCount());
        if (totalPopulationHistory.size() > HISTORY_SIZE * 2) {
            totalPopulationHistory.removeFirst();
        }

        Map<SpeciesKey, Integer> currentCounts = island.getSpeciesCounts();
        for (SpeciesKey species : SpeciesKey.values()) {
            populationHistory.putIfAbsent(species, new LinkedList<>());
            LinkedList<Integer> history = populationHistory.get(species);
            history.add(currentCounts.getOrDefault(species, 0));
            if (history.size() > HISTORY_SIZE) {
                history.removeFirst();
            }
        }
    }

    private void renderStatsWithGraphs(StringBuilder sb, Map<SpeciesKey, Integer> currentCounts) {
        int col = 0;
        List<SpeciesKey> allSpecies = new ArrayList<>(currentCounts.keySet());
        Collections.sort(allSpecies);

        for (SpeciesKey species : allSpecies) {
            int count = currentCounts.getOrDefault(species, 0);
            String icon = ICONS.getOrDefault(species, "?");
            String graph = ViewUtils.getSparkline(populationHistory.get(species), HISTORY_SIZE);
            
            sb.append(String.format("%s %-11s: %-5d %s  ", icon, species.getCode(), count, graph));
            if (++col % 2 == 0) {
                sb.append(CLEAR_EOL).append("\n");
            }
        }
        if (col % 2 != 0) {
            sb.append(CLEAR_EOL).append("\n");
        }
    }

    private void renderCell(StringBuilder sb, Cell cell) {
        cellBiomassMap.clear();
        boolean hasBiomass = false;
        
        for (Animal a : cell.getAnimals()) {
            if (a.isAlive()) {
                cellBiomassMap.merge(a.getSpeciesKey(), a.getWeight(), Double::sum);
                hasBiomass = true;
            }
        }

        for (Biomass p : cell.getBiomassContainers()) {
            if (p.isAlive() && p.getBiomass() > 0) {
                cellBiomassMap.merge(p.getSpeciesKey(), p.getBiomass(), Double::sum);
                hasBiomass = true;
            }
        }

        if (hasBiomass) {
            SpeciesKey topSpecies = null;
            double topWeight = -1.0;
            
            for (Map.Entry<SpeciesKey, Double> entry : cellBiomassMap.entrySet()) {
                if (entry.getValue() > topWeight) {
                    topWeight = entry.getValue();
                    topSpecies = entry.getKey();
                }
            }
            
            if (topSpecies != null) {
                String icon = ICONS.getOrDefault(topSpecies, "?");
                
                if (isGreen(topSpecies)) {
                    sb.append(GREEN).append(icon).append(RESET).append(" ");
                } else {
                    sb.append(icon).append(" ");
                }
            } else {
                sb.append("🏜️ ");
            }
        } else {
            sb.append("🏜️ ");
        }
    }

    private boolean isGreen(SpeciesKey key) {
        return key == SpeciesKey.PLANT || key == SpeciesKey.GRASS || key == SpeciesKey.CABBAGE;
    }
}