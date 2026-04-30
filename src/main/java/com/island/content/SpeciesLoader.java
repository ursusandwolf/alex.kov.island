package com.island.content;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.island.config.SimulationConstants.SCALE_1M;

/**
 * Loads species configuration from properties file.
 * Converts double properties to integer-based formats (SCALE_1M or percent 0-100).
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
        Map<SpeciesKey, Long> plantWeights = new HashMap<>();
        Map<SpeciesKey, Integer> plantMaxCounts = new HashMap<>();
        Map<SpeciesKey, Integer> plantSpeeds = new HashMap<>();

        // Discover species from list
        String listStr = props.getProperty("species.list", "");
        if (!listStr.isEmpty()) {
            for (String code : listStr.split(",")) {
                String trimmed = code.trim();
                boolean isPred = Boolean.parseBoolean(props.getProperty(trimmed + ".isPredator", "false"));
                SpeciesKey.fromCode(trimmed, isPred);
            }
        }

        // Load all registered species
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
                           Map<SpeciesKey, Long> plantWeights, 
                           Map<SpeciesKey, Integer> plantMaxCounts,
                           Map<SpeciesKey, Integer> plantSpeeds) {
        String code = key.getCode();
        long weight = toScaledLong(props.getProperty(code + ".weight", "1"));
        int maxCount = Math.max(0, Integer.parseInt(props.getProperty(code + ".maxPerCell", "1")));
        int speed = Math.max(0, Integer.parseInt(props.getProperty(code + ".speed", "0")));
        
        boolean isPlant = Boolean.parseBoolean(props.getProperty(code + ".isPlant", "false"));
        boolean isBiomass = Boolean.parseBoolean(props.getProperty(code + ".isBiomass", "false"));
        boolean isColdBlooded = Boolean.parseBoolean(props.getProperty(code + ".isColdBlooded", "false"));
        boolean isPackHunter = Boolean.parseBoolean(props.getProperty(code + ".isPackHunter", "false"));
        
        int presenceChance = toPercent(props.getProperty(code + ".presenceProb", "0.2"));
        long settlementBase = toScaledLong(props.getProperty(code + ".settlementBase", "0.1"));
        long settlementRange = toScaledLong(props.getProperty(code + ".settlementRange", "0.1"));

        SizeClass sizeClass = SizeClass.fromWeight((double) weight / SCALE_1M);
        int defaultReproChance = switch (sizeClass) {
            case TINY -> 25;
            case SMALL -> 18;
            case NORMAL -> 12;
            case MEDIUM -> 8;
            case LARGE -> 4;
            case HUGE -> 2;
        };
        int reproChance = toPercent(props.getProperty(code + ".reproductionChance", null), defaultReproChance);

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
        
        long food = toScaledLong(props.getProperty(code + ".foodForSaturation", "1"));
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
                .presenceChance(presenceChance)
                .settlementBase(settlementBase)
                .settlementRange(settlementRange)
                .build();
        
        if (isPlant || isBiomass) {
            biomassTypes.put(key, type);
        } else {
            animalTypes.put(key, type);
        }
    }

    private long toScaledLong(String val) {
        return (long) (Double.parseDouble(val) * SCALE_1M);
    }

    private int toPercent(String val) {
        return (int) (Double.parseDouble(val) * 100);
    }

    private int toPercent(String val, int defaultVal) {
        if (val == null) {
            return defaultVal;
        }
        return (int) (Double.parseDouble(val) * 100);
    }
}
