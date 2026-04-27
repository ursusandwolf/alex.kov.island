package com.island.content;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads species configuration from properties file.
 */
public class SpeciesLoader {
    private static final String CONFIG_FILE = "species.properties";

    public SpeciesRegistry load() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("Error loading species config: " + e.getMessage());
        }

        Map<SpeciesKey, AnimalType> animalTypes = new EnumMap<>(SpeciesKey.class);
        Map<SpeciesKey, Double> plantWeights = new EnumMap<>(SpeciesKey.class);
        Map<SpeciesKey, Integer> plantMaxCounts = new EnumMap<>(SpeciesKey.class);
        Map<SpeciesKey, Integer> plantSpeeds = new EnumMap<>(SpeciesKey.class);

        for (SpeciesKey key : SpeciesKey.values()) {
            String code = key.getCode();
            if (props.containsKey(code + ".weight")) {
                loadEntry(key, props, animalTypes, plantWeights, plantMaxCounts, plantSpeeds);
            }
        }

        return new SpeciesRegistry(animalTypes, plantWeights, plantMaxCounts, plantSpeeds);
    }

    private void loadEntry(SpeciesKey key, Properties props, 
                           Map<SpeciesKey, AnimalType> animalTypes,
                           Map<SpeciesKey, Double> plantWeights, 
                           Map<SpeciesKey, Integer> plantMaxCounts,
                           Map<SpeciesKey, Integer> plantSpeeds) {
        String code = key.getCode();
        double weight = Math.max(0.001, Double.parseDouble(props.getProperty(code + ".weight", "1")));
        int maxCount = Math.max(0, Integer.parseInt(props.getProperty(code + ".maxPerCell", "1")));
        int speed = Math.max(0, Integer.parseInt(props.getProperty(code + ".speed", "0")));
        
        if (key.isPlant()) {
            plantWeights.put(key, weight);
            plantMaxCounts.put(key, maxCount);
            plantSpeeds.put(key, speed);
        } else {
            double food = Math.max(0, Double.parseDouble(props.getProperty(code + ".foodForSaturation", "1")));
            int lifespan = Math.max(1, Integer.parseInt(props.getProperty(code + ".lifespan", "100")));
            
            String preyStr = props.getProperty(code + ".prey", "");
            Map<SpeciesKey, Integer> preyMap = new EnumMap<>(SpeciesKey.class);
            if (!preyStr.isEmpty()) {
                for (String part : preyStr.split(",")) {
                    String[] sub = part.split(":");
                    if (sub.length == 2) {
                        preyMap.put(SpeciesKey.fromCode(sub[0]), Integer.parseInt(sub[1]));
                    }
                }
            }
            
            animalTypes.put(key, new AnimalType(key, code, weight, maxCount, speed, food, lifespan, preyMap));
        }
    }
}
