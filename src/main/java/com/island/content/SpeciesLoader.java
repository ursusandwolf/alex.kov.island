package com.island.content;

import java.io.InputStream;
import java.util.HashMap;
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

        Map<SpeciesKey, AnimalType> animalTypes = new HashMap<>();
        Map<SpeciesKey, AnimalType> biomassTypes = new HashMap<>();
        Map<SpeciesKey, Double> plantWeights = new HashMap<>();
        Map<SpeciesKey, Integer> plantMaxCounts = new HashMap<>();
        Map<SpeciesKey, Integer> plantSpeeds = new HashMap<>();

        // 1. Discover species from list or use existing values
        String listStr = props.getProperty("species.list", "");
        if (!listStr.isEmpty()) {
            for (String code : listStr.split(",")) {
                String trimmed = code.trim();
                boolean isPred = Boolean.parseBoolean(props.getProperty(trimmed + ".isPredator", "false"));
                SpeciesKey.fromCode(trimmed, isPred);
            }
        }

        // 2. Load all registered species
        for (SpeciesKey key : SpeciesKey.values()) {
            String code = key.getCode();
            if (props.containsKey(code + ".weight")) {
                loadEntry(key, props, animalTypes, biomassTypes, plantWeights, plantMaxCounts, plantSpeeds);
            }
        }

        return new SpeciesRegistry(
                java.util.Collections.unmodifiableMap(animalTypes),
                java.util.Collections.unmodifiableMap(biomassTypes),
                java.util.Collections.unmodifiableMap(plantWeights),
                java.util.Collections.unmodifiableMap(plantMaxCounts),
                java.util.Collections.unmodifiableMap(plantSpeeds)
        );
    }

    private void loadEntry(SpeciesKey key, Properties props, 
                           Map<SpeciesKey, AnimalType> animalTypes,
                           Map<SpeciesKey, AnimalType> biomassTypes,
                           Map<SpeciesKey, Double> plantWeights, 
                           Map<SpeciesKey, Integer> plantMaxCounts,
                           Map<SpeciesKey, Integer> plantSpeeds) {
        String code = key.getCode();
        double weight = Math.max(0.001, Double.parseDouble(props.getProperty(code + ".weight", "1")));
        int maxCount = Math.max(0, Integer.parseInt(props.getProperty(code + ".maxPerCell", "1")));
        int speed = Math.max(0, Integer.parseInt(props.getProperty(code + ".speed", "0")));
        
        boolean isPlant = Boolean.parseBoolean(props.getProperty(code + ".isPlant", "false"));
        boolean isBiomass = Boolean.parseBoolean(props.getProperty(code + ".isBiomass", "false"));
        boolean isColdBlooded = Boolean.parseBoolean(props.getProperty(code + ".isColdBlooded", "false"));
        boolean isPackHunter = Boolean.parseBoolean(props.getProperty(code + ".isPackHunter", "false"));
        
        double presenceProb = Double.parseDouble(props.getProperty(code + ".presenceProb", "0.2"));
        double settlementBase = Double.parseDouble(props.getProperty(code + ".settlementBase", "0.1"));
        double settlementRange = Double.parseDouble(props.getProperty(code + ".settlementRange", "0.1"));

        SizeClass sizeClass = SizeClass.fromWeight(weight);
        double defaultReproChance = switch (sizeClass) {
            case TINY -> 0.25;
            case SMALL -> 0.18;
            case NORMAL -> 0.12;
            case MEDIUM -> 0.08;
            case LARGE -> 0.04;
            case HUGE -> 0.02;
        };
        double reproChance = Double.parseDouble(props.getProperty(code + ".reproductionChance", String.valueOf(defaultReproChance)));

        int defaultMaxOffspring = switch (sizeClass) {
            case TINY -> 10;
            case SMALL -> 6;
            case NORMAL -> 4;
            case MEDIUM -> 2;
            case LARGE -> 1;
            case HUGE -> 1;
        };
        int maxOffspring = Integer.parseInt(props.getProperty(code + ".maxOffspring", String.valueOf(defaultMaxOffspring)));

        if (isPlant) {
            plantWeights.put(key, weight);
            plantMaxCounts.put(key, maxCount);
            plantSpeeds.put(key, speed);
        }
        
        double food = Math.max(0, Double.parseDouble(props.getProperty(code + ".foodForSaturation", "1")));
        int lifespan = Math.max(1, Integer.parseInt(props.getProperty(code + ".lifespan", "100")));
        
        String preyStr = props.getProperty(code + ".prey", "");
        Map<SpeciesKey, Integer> preyMap = new HashMap<>();
        if (!preyStr.isEmpty()) {
            for (String part : preyStr.split(",")) {
                String[] sub = part.split(":");
                if (sub.length == 2) {
                    preyMap.put(SpeciesKey.fromCode(sub[0]), Integer.parseInt(sub[1]));
                }
            }
        }
        
        AnimalType type = AnimalType.builder()
                .speciesKey(key)
                .typeName(code)
                .weight(weight)
                .maxPerCell(maxCount)
                .speed(speed)
                .foodForSaturation(food)
                .maxEnergy(food)
                .maxLifespan(lifespan)
                .huntProbabilities(java.util.Collections.unmodifiableMap(preyMap))
                .isPredator(key.isPredator())
                .sizeClass(sizeClass)
                .isColdBlooded(isColdBlooded)
                .isPackHunter(isPackHunter)
                .isBiomass(isBiomass)
                .isPlant(isPlant)
                .reproductionChance(reproChance)
                .maxOffspring(maxOffspring)
                .presenceProb(presenceProb)
                .settlementBase(settlementBase)
                .settlementRange(settlementRange)
                .build();
        
        if (isPlant || isBiomass) {
            biomassTypes.put(key, type);
        } else {
            animalTypes.put(key, type);
        }
    }
}
