package com.island.view;

import com.island.content.Animal;
import com.island.content.SpeciesKey;
import com.island.content.Biomass;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.ViewUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Console visualization of the island ecosystem.
 */
public class ConsoleView {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static final Map<SpeciesKey, String> ICONS = new EnumMap<>(SpeciesKey.class);

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
        ICONS.put(SpeciesKey.CATERPILLAR, "🐛");
        ICONS.put(SpeciesKey.BUTTERFLY, "🦋");
        ICONS.put(SpeciesKey.PLANT, "🌿");
        ICONS.put(SpeciesKey.GRASS, "☘️");
        ICONS.put(SpeciesKey.CABBAGE, "🥬");
    }

    private boolean isSilent = false;
    private final LinkedList<Integer> totalPopulationHistory = new LinkedList<>();
    private final Map<SpeciesKey, LinkedList<Integer>> populationHistory = new EnumMap<>(SpeciesKey.class);
    private static final int HISTORY_SIZE = 10;
    private int lastRenderedTick = -1;
    private static final int RENDER_THROTTLE = 1;

    public void setSilent(boolean silent) {
        this.isSilent = silent;
    }

    public void display(Island island) {
        if (isSilent) {
            return;
        }
        lastRenderedTick = island.getTickCount();

        updateHistory(island);
        
        StringBuilder sb = new StringBuilder();
        sb.append("\033[H\033[2J"); // Clear console
        sb.append(CYAN).append("=== ISLAND ECOSYSTEM DASHBOARD ===").append(RESET).append("\n");
        
        String totalGraph = ViewUtils.getSparkline(totalPopulationHistory, HISTORY_SIZE * 2);
        sb.append(String.format("Tick: %d | Total Organisms: %d %s\n", 
                island.getTickCount(), island.getTotalOrganismCount(), totalGraph));
        
        // Hunger Stats
        double satiety = island.getGlobalSatiety();
        int starving = island.getStarvingCount();
        String satietyColor = satiety > 70 ? GREEN : (satiety > 40 ? YELLOW : "\u001B[31m"); 
        
        sb.append(String.format("Global Satiety: %s%3.1f%%%s [", satietyColor, satiety, RESET));
        int progress = (int) (satiety / 5);
        sb.append(satietyColor).append("#".repeat(progress)).append(".".repeat(20 - progress)).append(RESET).append("] ");
        sb.append(String.format("| Starving: %s%d%s\n", (starving > 0 ? "\u001B[31m" : GREEN), starving, RESET));
        
        int hungerTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.HUNGER);
        int ageTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.AGE);
        int eatenTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.EATEN);
        int exhaustTotal = island.getTotalAnimalDeathCount(com.island.content.DeathCause.MOVEMENT_EXHAUSTION);

        sb.append(String.format("Total Deaths: Hunger: %s%d%s | Old Age: %s%d%s | Exhausted: %s%d%s\n", 
                "\u001B[31m", hungerTotal, RESET, 
                YELLOW, ageTotal, RESET,
                "\u001B[35m", exhaustTotal, RESET)); 
        sb.append(String.format("Total Hunts (Eaten Animals): %s%d%s\n", 
                GREEN, eatenTotal, RESET));

        sb.append("-".repeat(60)).append("\n");

        Map<SpeciesKey, Integer> currentCounts = new TreeMap<>(island.getSpeciesCounts());

        renderStatsWithGraphs(sb, currentCounts);

        sb.append("-".repeat(60)).append("\n");
        sb.append(YELLOW).append("Map View:").append(RESET).append("\n");
        
        for (int y = 0; y < island.getHeight(); y++) {
            for (int x = 0; x < island.getWidth(); x++) {
                renderCell(sb, island.getGrid()[x][y]);
            }
            sb.append("\n");
        }
        
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
                sb.append("\n");
            }
        }
        if (col % 2 != 0) {
            sb.append("\n");
        }
    }

    private void renderCell(StringBuilder sb, Cell cell) {
        Map<SpeciesKey, Double> biomassMap = new EnumMap<>(SpeciesKey.class);
        
        for (Animal a : cell.getAnimals()) {
            if (a.isAlive()) {
                biomassMap.put(a.getSpeciesKey(), 
                        biomassMap.getOrDefault(a.getSpeciesKey(), 0.0) + a.getWeight());
            }
        }

        for (Biomass p : cell.getBiomassContainers()) {
            if (p.isAlive()) {
                biomassMap.put(p.getSpeciesKey(), 
                        biomassMap.getOrDefault(p.getSpeciesKey(), 0.0) + p.getBiomass());
            }
        }

        if (!biomassMap.isEmpty()) {
            List<SpeciesKey> topSpeciesList = biomassMap.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
            
            int displayIndex = (lastRenderedTick / RENDER_THROTTLE) % topSpeciesList.size();
            SpeciesKey speciesToDisplay = topSpeciesList.get(displayIndex);
            String icon = ICONS.getOrDefault(speciesToDisplay, "?");
            
            if (isGreen(speciesToDisplay)) {
                sb.append(GREEN).append(icon).append(RESET).append(" ");
            } else {
                sb.append(icon).append(" ");
            }
        } else {
            sb.append(". ");
        }
    }

    private boolean isGreen(SpeciesKey key) {
        return key == SpeciesKey.PLANT || key == SpeciesKey.GRASS || key == SpeciesKey.CABBAGE;
    }
}
