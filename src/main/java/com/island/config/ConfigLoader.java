package com.island.config;

import com.island.content.SpeciesConfig;
import com.island.content.SpeciesKey;
import com.island.util.InteractionMatrix;

/**
 * Handles loading of all simulation configurations.
 */
public class ConfigLoader {
    public Configuration loadGeneralConfig() {
        return Configuration.load();
    }

    public InteractionMatrix loadInteractionMatrix(SpeciesConfig speciesConfig) {
        InteractionMatrix matrix = new InteractionMatrix();
        for (SpeciesKey predatorKey : SpeciesKey.values()) {
            for (SpeciesKey preyKey : SpeciesKey.values()) {
                int chance = speciesConfig.getHuntProbability(predatorKey.getCode(), preyKey.getCode());
                if (chance > 0) {
                    matrix.setChance(predatorKey, preyKey, chance);
                }
            }
            // Default plant eating chance
            int plantChance = speciesConfig.getHuntProbability(predatorKey.getCode(), "plant");
            if (plantChance > 0) {
                matrix.setChance(predatorKey, SpeciesKey.PLANT, plantChance);
            } else if (predatorKey.isPredator() == false) {
                // Default fallback for herbivores if not specified
                matrix.setChance(predatorKey, SpeciesKey.PLANT, 100);
            }
        }
        return matrix;
    }
}
