package com.island.nature.config;

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
            }
        } catch (Exception e) {
            System.err.println("Error loading configuration, using defaults: " + e.getMessage());
        }
        
        // System properties have priority
        config.islandWidth = getIntProperty(props, "island.width", 8);
        config.islandHeight = getIntProperty(props, "island.height", 8);
        config.tickDurationMs = getIntProperty(props, "island.tickDurationMs", 100);
        
        return config;
    }

    private static int getIntProperty(Properties props, String key, int defaultValue) {
        String sysValue = System.getProperty(key);
        if (sysValue != null) {
            return Integer.parseInt(sysValue);
        }
        String propValue = props.getProperty(key);
        if (propValue != null) {
            return Integer.parseInt(propValue);
        }
        return defaultValue;
    }
}
