package com.island.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and provides access to general simulation parameters.
 */
public class Configuration {
    private int islandWidth = 100;
    private int islandHeight = 20;
    private int tickDurationMs = 1000;

    public int getIslandWidth() {
        return islandWidth;
    }

    public int getIslandHeight() {
        return islandHeight;
    }

    public int getTickDurationMs() {
        return tickDurationMs;
    }

    public static Configuration load() {
        Configuration config = new Configuration();
        Properties props = new Properties();
        try (InputStream is = Configuration.class.getClassLoader().getResourceAsStream("species.properties")) {
            if (is != null) {
                props.load(is);
                config.islandWidth = Math.max(1, Integer.parseInt(props.getProperty("island.width", "100")));
                config.islandHeight = Math.max(1, Integer.parseInt(props.getProperty("island.height", "20")));
                config.tickDurationMs = Math.max(1, Integer.parseInt(props.getProperty("island.tickDurationMs", "1000")));
            }
        } catch (Exception e) {
            System.err.println("Error loading configuration, using defaults: " + e.getMessage());
        }
        return config;
    }
}
