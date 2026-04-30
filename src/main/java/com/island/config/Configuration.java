package com.island.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and provides access to general simulation parameters.
 */
public class Configuration {
    private int islandWidth = 10;
    private int islandHeight = 10;
    private int tickDurationMs = 100;

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
                config.islandWidth = Math.max(1, Integer.parseInt(props.getProperty("island.width", "8")));
                config.islandHeight = Math.max(1, Integer.parseInt(props.getProperty("island.height", "8")));
                config.tickDurationMs = Math.max(1, Integer.parseInt(props.getProperty("island.tickDurationMs", "100")));
            }
        } catch (Exception e) {
            System.err.println("Error loading configuration, using defaults: " + e.getMessage());
        }
        return config;
    }
}
