package com.island.content;

import lombok.Getter;
import java.io.InputStream;
import java.util.*;

/**
 * Loads and provides access to species configuration.
 */
@Getter
public final class SpeciesConfig {
    private static final SpeciesConfig INSTANCE = new SpeciesConfig();
    private final Map<String, AnimalType> animalTypes = new HashMap<>();

    // Plant constants (redundant but used in some classes)
    private double plantWeight = 1.0;
    private int plantMaxCount = 200;
    private double cabbageWeight = 2.0;
    private int cabbageMaxCount = 100;

    private SpeciesConfig() {
        loadFromProperties();
    }

    public static SpeciesConfig getInstance() { return INSTANCE; }

    private void loadFromProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("species.properties")) {
            if (is == null) return;
            props.load(is);
            
            // Hardcoded initial list of species keys
            String[] speciesKeys = {
                "wolf", "boa", "fox", "bear", "eagle",
                "horse", "deer", "rabbit", "mouse", "goat", "sheep", "boar", "buffalo", "duck", "caterpillar"
            };

            for (String key : speciesKeys) {
                double weight = Double.parseDouble(props.getProperty(key + ".weight", "1"));
                int maxCount = Integer.parseInt(props.getProperty(key + ".maxPerCell", "1"));
                int speed = Integer.parseInt(props.getProperty(key + ".speed", "1"));
                double food = Double.parseDouble(props.getProperty(key + ".foodForSaturation", "1"));
                int lifespan = Integer.parseInt(props.getProperty(key + ".lifespan", "100"));
                
                String preyStr = props.getProperty(key + ".prey", "");
                Map<String, Integer> prey = new HashMap<>();
                if (!preyStr.isEmpty()) {
                    for (String part : preyStr.split(",")) {
                        String[] pair = part.split(":");
                        prey.put(pair[0], Integer.parseInt(pair[1]));
                    }
                }
                
                animalTypes.put(key, new AnimalType(key, key.substring(0, 1).toUpperCase() + key.substring(1), 
                        weight, maxCount, speed, food, lifespan, prey));
            }
            
            // Load specific plant weights if needed
            plantWeight = Double.parseDouble(props.getProperty("plant.weight", "1.0"));
            plantMaxCount = Integer.parseInt(props.getProperty("plant.maxPerCell", "200"));
            cabbageWeight = Double.parseDouble(props.getProperty("cabbage.weight", "2.0"));
            cabbageMaxCount = Integer.parseInt(props.getProperty("cabbage.maxPerCell", "100"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AnimalType getAnimalType(String key) { return animalTypes.get(key); }
    
    public Set<String> getAllSpeciesKeys() {
        Set<String> allKeys = new HashSet<>(animalTypes.keySet());
        allKeys.add("plant");
        allKeys.add("cabbage");
        allKeys.add("caterpillar");
        return allKeys;
    }

    public int getHuntProbability(String predator, String prey) {
        AnimalType type = animalTypes.get(predator);
        return (type != null) ? type.getHuntProbability(prey) : 0;
    }

    // Manual getters to bypass potential Lombok friction in some environments
    public double getPlantWeight() { return plantWeight; }
    public int getPlantMaxCount() { return plantMaxCount; }
    public double getCabbageWeight() { return cabbageWeight; }
    public int getCabbageMaxCount() { return cabbageMaxCount; }
}
