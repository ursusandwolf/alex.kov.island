package com.island.simcity.view;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;
import com.island.util.common.ViewUtils;

/**
 * Rich Console visualization for SimCity.
 */
public class CityConsoleView {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CLEAR_EOL = "\u001B[K";
    private static final String CURSOR_HOME = "\u001B[H";
    private static final String CLEAR_DOWN = "\u001B[J";

    private static final Map<String, String> ICONS = new HashMap<>();

    static {
        ICONS.put("residential", "🏘️");
        ICONS.put("commercial", "🏪");
        ICONS.put("industrial", "🏭");
        ICONS.put("agricultural", "🚜");
        ICONS.put("road", "🛣️");
        ICONS.put("railway", "🛤️");
        ICONS.put("metro", "🚇");
        ICONS.put("water_pipe", "🚰");
        ICONS.put("power_plant", "⚡");
        ICONS.put("power_line", "🗼");
    }

    private final LinkedList<Integer> moneyHistory = new LinkedList<>();
    private static final int HISTORY_SIZE = 20;

    public void render(WorldSnapshot snapshot) {
        Map<String, Number> metrics = snapshot.getMetrics();
        int money = metrics.getOrDefault("money", 0).intValue();
        moneyHistory.add(money);
        if (moneyHistory.size() > HISTORY_SIZE) {
            moneyHistory.removeFirst();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CURSOR_HOME);
        sb.append(CYAN).append("=== SIMCITY DASHBOARD ===").append(RESET).append(CLEAR_EOL).append("\n");
        
        String moneyGraph = ViewUtils.getSparkline(moneyHistory, HISTORY_SIZE);
        long income = metrics.getOrDefault("income", 0).longValue();
        long expenses = metrics.getOrDefault("expenses", 0).longValue();
        long net = income - expenses;
        String netColor = net >= 0 ? GREEN : RED;

        sb.append(String.format("Tick: %d | Population: %d | Money: %d %s", 
                snapshot.getTickCount(), snapshot.getTotalEntityCount(), money, moneyGraph)).append(CLEAR_EOL).append("\n");
        
        sb.append(String.format("Last Tick: %s%+d%s (Income: %d, Expenses: %d)", 
                netColor, net, RESET, income, expenses)).append(CLEAR_EOL).append("\n");

        sb.append(String.format("Demand: R:%s%d%s C:%s%d%s I:%s%d%s", 
                YELLOW, metrics.getOrDefault("resDemand", 0), RESET,
                CYAN, metrics.getOrDefault("comDemand", 0), RESET,
                PURPLE, metrics.getOrDefault("indDemand", 0), RESET)).append(CLEAR_EOL).append("\n");

        sb.append("-".repeat(snapshot.getWidth() * 3 + 2)).append(CLEAR_EOL).append("\n");

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

    private void renderNode(StringBuilder sb, NodeSnapshot node) {
        if (node != null && node.hasOrganisms()) {
            String species = node.getTopSpeciesCode();
            String icon = ICONS.getOrDefault(species, "❓");
            sb.append(icon).append(" ");
        } else {
            sb.append("⬜ "); // Empty/Untouched land
        }
    }
}
