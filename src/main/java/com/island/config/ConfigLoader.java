package com.island.config;

import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import com.island.util.InteractionMatrix;

/**
 * Handles loading of all simulation configurations.
 */
public class ConfigLoader {
    public Configuration loadGeneralConfig() {
        return Configuration.load();
    }

    public InteractionMatrix loadInteractionMatrix(SpeciesRegistry registry) {
        InteractionMatrix matrix = new InteractionMatrix();
        for (SpeciesKey predatorKey : SpeciesKey.values()) {
            for (SpeciesKey preyKey : SpeciesKey.values()) {
                int chance = registry.getHuntProbability(predatorKey, preyKey);
                if (chance > 0) {
                    matrix.setChance(predatorKey, preyKey, chance);
                }
            }
            // Default plant eating chance
            int plantChance = registry.getHuntProbability(predatorKey, SpeciesKey.PLANT);
            if (plantChance > 0) {
                matrix.setChance(predatorKey, SpeciesKey.PLANT, plantChance);
            } else if (!predatorKey.isPredator() && predatorKey != SpeciesKey.PLANT 
                    && predatorKey != SpeciesKey.GRASS && predatorKey != SpeciesKey.CABBAGE) {
                // Default fallback for herbivores if not specified (excluding plants themselves)
                matrix.setChance(predatorKey, SpeciesKey.PLANT, 100);
            }
        }
        return matrix;
    }
}
