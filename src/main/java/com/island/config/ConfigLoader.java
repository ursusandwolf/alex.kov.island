package com.island.config;

import com.island.content.SpeciesConfig;
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
        for (String predatorKey : speciesConfig.getAllSpeciesKeys()) {
            for (String preyKey : speciesConfig.getAllSpeciesKeys()) {
                int chance = speciesConfig.getHuntProbability(predatorKey, preyKey);
                if (chance > 0) {
                    matrix.setChance(predatorKey, preyKey, chance);
                }
            }
            // Default plant eating chance
            int plantChance = speciesConfig.getHuntProbability(predatorKey, "plant");
            if (plantChance > 0) {
                matrix.setChance(predatorKey, "Plant", plantChance);
            } else if (isDefaultHerbivore(predatorKey)) {
                matrix.setChance(predatorKey, "Plant", 100);
            }
        }
        return matrix;
    }

    private boolean isDefaultHerbivore(String key) {
        return key.equals("rabbit") || key.equals("duck") || key.equals("goat") 
            || key.equals("sheep") || key.equals("horse") || key.equals("deer")
            || key.equals("cow") || key.equals("buffalo");
    }
}
