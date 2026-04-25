package com.island.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for UI and console visualization helpers.
 */
public final class ViewUtils {
    
    private static final char[] SPARK_CHARS = {' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'};

    private ViewUtils() {}

    public static String getSparkline(List<Integer> history, int width) {
        if (history == null || history.size() < 2) {
            return " ".repeat(width);
        }
        
        int min = history.stream().min(Integer::compare).orElse(0);
        int max = history.stream().max(Integer::compare).orElse(1);
        int range = Math.max(1, max - min);

        StringBuilder sparkline = new StringBuilder();
        for (int val : history) {
            int idx = (int) (((double) (val - min) / range) * (SPARK_CHARS.length - 1));
            sparkline.append(SPARK_CHARS[idx]);
        }
        
        // Pad to consistent width
        while (sparkline.length() < width) {
            sparkline.insert(0, " ");
        }
        return sparkline.toString();
    }
}
