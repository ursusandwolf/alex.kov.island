package com.island.nature.config;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.island.nature.entities.core.Biomass;

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

    // Climate System
    private boolean climateEnabled = true;
    private int baseTemperature = 20; // Celsius
    private int winterTemperatureDelta = -30;
    private int summerTemperatureDelta = 15;
    private int temperatureFluctuationRange = 5;

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
    private int coldBloodedHeatStressBP = 12000;
    private int warmBloodedColdStressBP = 15000;
    private int warmBloodedHeatStressBP = 13000;
    private int endangeredMetabolismReductionBP = 5000;

    // Temperature Thresholds
    private int hibernationTempThreshold = 10;
    private int heatStressTempThreshold = 35;
    private int coldStressTempThreshold = 0;

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

    // specialized species rates
    private int caterpillarMetabolismRateBP = 500;
    private int caterpillarFeedEfficiencyBP = 10000;
    private int butterflyReproductionRateBP = 1000;

    // Partitioning
    private int partitioningSmallWorldThreshold = 64;
    private int partitioningSmallWorldTasks = 16;
    private int partitioningMediumWorldMultiplier = 16;
    private int partitioningMediumWorldTasksMultiplier = 2;
    private int partitioningLargeWorldTasksMultiplier = 4;
    private int partitioningLargeWorldThreshold = 1000;
    private int partitioningMaxChunkSize = 32;

    // Dynamic Load Balancing
    private boolean dynamicChunkingEnabled = false;
    private int rebalanceInterval = 10;
    private int dynamicChunkingTargetLoad = 500;
    private int dynamicChunkingMinSize = 2;

    // Simulation monitoring
    private long maxSimulationDurationMs = 5 * 60 * 1000;
    private int monitoringIntervalMs = 2000;
    private long randomSeed = -1; // -1 means use current time

    // View
    private boolean headless = false;

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
