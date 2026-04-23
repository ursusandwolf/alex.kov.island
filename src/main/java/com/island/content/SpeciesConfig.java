package com.island.content;

import lombok.Getter;
import java.io.InputStream;
import java.util.*;

@Getter
public final class SpeciesConfig {
    private static final SpeciesConfig INSTANCE = new SpeciesConfig();
    private final Map<String, AnimalType> animalTypes = new HashMap<>();

    public static SpeciesConfig getInstance() { return INSTANCE; }

    private SpeciesConfig() {
        loadFromProperties();
    }

    private void loadFromProperties() {
        Properties prop = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("species.properties")) {
            if (input == null) return;
            prop.load(input);

            String[] species = {
                "wolf", "boa", "fox", "bear", "eagle", 
                "horse", "deer", "rabbit", "mouse", "goat", 
                "sheep", "boar", "buffalo", "duck", "caterpillar"
            };

            for (String s : species) {
                double weight = Double.parseDouble(prop.getProperty(s + ".weight", "0"));
                int maxPerCell = Integer.parseInt(prop.getProperty(s + ".maxPerCell", "0"));
                int speed = Integer.parseInt(prop.getProperty(s + ".speed", "0"));
                double food = Double.parseDouble(prop.getProperty(s + ".foodForSaturation", "0"));
                int lifespan = Integer.parseInt(prop.getProperty(s + ".lifespan", "100"));
                
                String preyStr = prop.getProperty(s + ".prey", "");
                Map<String, Integer> prey = new HashMap<>();
                if (!preyStr.isEmpty()) {
                    for (String entry : preyStr.split(",")) {
                        String[] parts = entry.split(":");
                        prey.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }

                animalTypes.put(s, new AnimalType(s, s.substring(0, 1).toUpperCase() + s.substring(1), 
                                                 weight, maxPerCell, speed, food, lifespan, prey));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AnimalType getAnimalType(String key) { return animalTypes.get(key); }
    public Set<String> getAllSpeciesKeys() { return animalTypes.keySet(); }
    public int getHuntProbability(String predator, String prey) {
        AnimalType type = animalTypes.get(predator);
        return (type != null) ? type.getHuntProbability(prey) : 0;
    }
}
