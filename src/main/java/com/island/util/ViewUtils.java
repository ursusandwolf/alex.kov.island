package com.island.util;

import java.util.List;

/**
 * Utility class for console visualization.
 */
public final class ViewUtils {
    
    private ViewUtils() {
    }

    public static String getSparkline(List<Integer> data, int width) {
        if (data == null || data.isEmpty()) {
            return " ".repeat(width);
        }

        char[] chars = {',', '.', '_', '▂', '▃', '▄', '-', '~', '=', '▅', '▆', '▇', '█'};
        int max = data.stream().max(Integer::compare).orElse(0);
        int min = data.stream().min(Integer::compare).orElse(0);
        int range = max - min;

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, data.size() - width);
        for (int i = start; i < data.size(); i++) {
            int val = data.get(i);
            int idx;
            if (range == 0) {
                idx = (val > 0) ? chars.length / 2 : 0;
            } else {
                idx = (int) ((double) (val - min) / range * (chars.length - 1));
            }
            sb.append(chars[idx]);
        }
        
        while (sb.length() < width) {
            sb.insert(0, ' ');
        }
        return sb.toString();
    }
}
