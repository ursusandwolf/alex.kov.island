package com.island.content;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import static com.island.config.SimulationConstants.SCALE_1M;

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

        String listStr = props.getProperty("species.list", "");
        if (!listStr.isEmpty()) {
            for (String code : listStr.split(",")) {
                String trimmed = code.trim();
                boolean isPred = Boolean.parseBoolean(props.getProperty(trimmed + ".isPredator", "false"));
                SpeciesKey.fromCode(trimmed, isPred);
            }
        }

        for (SpeciesKey key : SpeciesKey.values()) {
            if (props.containsKey(key.getCode() + ".weight")) {
                loadEntry(key, props, animalTypes, biomassTypes);
            }
        }

        return new SpeciesRegistry(
                Collections.unmodifiableMap(animalTypes),
                Collections.unmodifiableMap(biomassTypes)
        );
    }

    private void loadEntry(SpeciesKey key, Properties props, 
                           Map<SpeciesKey, AnimalType> animalTypes,
                           Map<SpeciesKey, AnimalType> biomassTypes) {
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

        long food = toScaledLong(props.getProperty(code + ".foodForSaturation", "1"));
        int lifespan = Math.max(1, Integer.parseInt(props.getProperty(code + ".lifespan", "100")));

        boolean canFly = Boolean.parseBoolean(props.getProperty(code + ".canFly", "false"));
        boolean canSwim = Boolean.parseBoolean(props.getProperty(code + ".canSwim", "false"));
        boolean canWalk = Boolean.parseBoolean(props.getProperty(code + ".canWalk", "true"));
        
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
                .speciesKey(key).typeName(code).weight(weight).maxPerCell(maxCount).speed(speed)
                .foodForSaturation(food).maxEnergy(food).maxLifespan(lifespan)
                .huntProbabilities(Collections.unmodifiableMap(preyMap))
                .isPredator(key.isPredator()).sizeClass(sizeClass)
                .isColdBlooded(isColdBlooded).isPackHunter(isPackHunter).isBiomass(isBiomass).isPlant(isPlant)
                .reproductionChance(reproChance).maxOffspring(maxOffspring)
                .presenceChance(presenceChance).settlementBase(settlementBase).settlementRange(settlementRange)
                .canFly(canFly).canSwim(canSwim).canWalk(canWalk)
                .build();
        
        if (isPlant || isBiomass) {
            biomassTypes.put(key, type);
            return;
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
