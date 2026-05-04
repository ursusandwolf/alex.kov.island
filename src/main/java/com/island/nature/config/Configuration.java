package com.island.nature.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Properties;

/**
 * Loads and provides access to simulation parameters.
 * Replaces static constants from SimulationConstants to allow multi-instance simulations.
 */
@Getter
@Setter
@Slf4j
public class Configuration {
    // Island dimensions
    private int islandWidth = 8;
    private int islandHeight = 8;
    private int tickDurationMs = 100;
    private int seasonDuration = 50;

    // Scaling factors
    private long scale1M = 1_000_000L;
    private int scale10K = 10_000;

    // Movement and Hunting costs (Basis Points)
    private int speedMoveCostStepBP = 50;
    private int preyRelativeSpeedHuntCostStepBP = 500;
    private int baseMetabolismBP = 100;

    // Metabolism modifiers (Basis Points)
    private int herbivoreMetabolismModifierBP = 5000;
    private int reptileMetabolismModifierBP = 4000;
    private int hibernationMetabolismModifierBP = 1000;

    // Hunting Logic (Basis Points and Percentages)
    private int huntStrikeCostPreyWeightBP = 1000;
    private int huntStrikeCostMaxEnergyCapBP = 50;
    private int huntRoiThresholdBP = 11000;
    private int predatorFailHuntPenaltyBP = 500;
    private int herbivoreFailFeedPenaltyBP = 300;
    private int overpopulationHuntBonusPercent = 15;

    // Pack Hunting
    private int wolfPackMinSize = 3;
    private int wolfPackMaxBonusPercent = 30;
    private int wolfPackBearHuntMaxChancePercent = 30;

    // Fatigue
    private int huntFatigueThreshold = 5;
    private int huntFatigueCostModifierBP = 13000;

    // Performance / LOD limits
    private int feedingLodLimit = 500;
    private int reproductionLodLimit = 30;
    private int movementLodLimit = 100;

    // Vital signs
    private long deathEnergyThreshold = 1L;
    private int hungerThresholdPercent = 30;
    private int reproductionMinEnergyPercent = 50;

    // Endangered species protection
    private int endangeredPopulationThresholdBP = 500;
    private int endangeredReproBonusBP = 2000;
    private int endangeredSpeedBonus = 2;
    private int endangeredMaxHideChancePercent = 60;
    private int endangeredMinHideChancePercent = 30;

    // Plant and Biomass
    private int herbivoreOffspringBonus = 1;
    private int plantInitialBiomassBP = 8000;
    private int plantGrowthRateBP = 8000;
    private int biomassMoveChunkBP = 2500;

    // Specialized species rates
    private int caterpillarMetabolismRateBP = 500;
    private int caterpillarFeedEfficiencyBP = 10000;
    private int butterflyReproductionRateBP = 1000;

    // Cold-blooded intervals
    private int coldBloodedMoveInterval = 2;
    private int coldBloodedFeedInterval = 3;
    private int coldBloodedReproInterval = 4;

    // Default chances
    private int defaultPredatorPresenceChance = 20;
    private int defaultHerbivorePresenceChance = 50;

    public static Configuration load() {
        Configuration config = new Configuration();
        Properties props = new Properties();
        try (InputStream is = Configuration.class.getClassLoader().getResourceAsStream("species.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            log.error("Failed to load species.properties, using defaults", e);
        }

        // Use reflection to load all fields that have a matching property
        for (java.lang.reflect.Field field : Configuration.class.getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            String propertyKey = "island." + field.getName();
            String value = System.getProperty(propertyKey);
            if (value == null) {
                value = props.getProperty(propertyKey);
            }

            if (value != null) {
                try {
                    field.setAccessible(true);
                    if (field.getType() == int.class) {
                        field.setInt(config, Integer.parseInt(value));
                    } else if (field.getType() == long.class) {
                        field.setLong(config, Long.parseLong(value));
                    } else if (field.getType() == boolean.class) {
                        field.setBoolean(config, Boolean.parseBoolean(value));
                    } else if (field.getType() == double.class) {
                        field.setDouble(config, Double.parseDouble(value));
                    } else {
                        log.warn("Unsupported config field type {} for field {}", field.getType(), field.getName());
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid value '{}' for config property '{}', using default", value, propertyKey);
                } catch (Exception e) {
                    log.error("Failed to set config field '{}' via reflection", field.getName(), e);
                }
            }
        }
        
        return config;
    }
}
