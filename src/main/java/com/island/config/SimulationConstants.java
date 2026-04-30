package com.island.config;

/**
 * Constants for simulation, converted to integer-based arithmetic.
 * SCALE_1M (1,000,000) is used for weights, energy, and biomass.
 * SCALE_10K (10,000) is used for rates, modifiers, and probabilities (basis points).
 */
public final class SimulationConstants {
    public static final long SCALE_1M = 1_000_000L;
    public static final int SCALE_10K = 10_000;
    public static final int PERCENT_100 = 100;

    // Energy Costs (Basis points: 100 = 1%)
    public static final int SPEED_MOVE_COST_STEP_BP = 100; // 1%
    public static final int PREY_RELATIVE_SPEED_HUNT_COST_STEP_BP = 500; // 5%
    public static final int BASE_METABOLISM_BP = 150; // 1.5%
    public static final int HERBIVORE_METABOLISM_MODIFIER_BP = 7000; // 70%
    public static final int REPTILE_METABOLISM_MODIFIER_BP = 4000; // 40%

    // Hunting Logic
    public static final int HUNT_STRIKE_COST_PREY_WEIGHT_BP = 1000; // 10%
    public static final int HUNT_STRIKE_COST_MAX_ENERGY_CAP_BP = 50; // 0.5%
    public static final int HUNT_ROI_THRESHOLD_BP = 11000; // 1.1 ratio (110%)

    // Wolf Pack Hunting
    public static final int WOLF_PACK_MIN_SIZE = 3;
    public static final int WOLF_PACK_MAX_BONUS_PERCENT = 30;
    public static final int WOLF_PACK_BEAR_HUNT_MAX_CHANCE_PERCENT = 30;

    // Hunt Fatigue Constants
    public static final int HUNT_FATIGUE_THRESHOLD = 5; 
    public static final int HUNT_FATIGUE_COST_MODIFIER_BP = 13000; // 1.3 ratio
    
    // Feeding Penalties & Bonuses
    public static final int PREDATOR_FAIL_HUNT_PENALTY_BP = 500; // 5%
    public static final int HERBIVORE_FAIL_FEED_PENALTY_BP = 300; // 3%
    public static final int OVERPOPULATION_HUNT_BONUS_PERCENT = 15;

    // LOD Sampling
    public static final int FEEDING_LOD_LIMIT = 500;
    public static final int REPRODUCTION_LOD_LIMIT = 30;
    public static final int MOVEMENT_LOD_LIMIT = 100;

    // Thresholds
    public static final long DEATH_ENERGY_THRESHOLD = 1L; // Minimal unit
    public static final int STARVATION_THRESHOLD_PERCENT = 30;
    public static final int REPRODUCTION_MIN_ENERGY_PERCENT = 50; 

    // Red Book / Endangered Protection
    public static final int ENDANGERED_POPULATION_THRESHOLD_BP = 500; // 5%
    public static final int ENDANGERED_REPRO_BONUS_BP = 2000; // 20%
    public static final int ENDANGERED_SPEED_BONUS = 2;
    public static final int ENDANGERED_MAX_HIDE_CHANCE_PERCENT = 60;
    public static final int ENDANGERED_MIN_HIDE_CHANCE_PERCENT = 30;

    // Reproduction Scaling
    public static final int HERBIVORE_OFFSPRING_BONUS = 1; 

    // Plants & Biomass logic
    public static final int PLANT_INITIAL_BIOMASS_BP = 8000; // 80%
    public static final int PLANT_GROWTH_RATE_BP = 8000; // 80%
    public static final int BIOMASS_MOVE_CHUNK_BP = 2500; // 25%

    // Caterpillar Pendulum Constants
    public static final int CATERPILLAR_METABOLISM_RATE_BP = 500; // 5%
    public static final int CATERPILLAR_FEED_EFFICIENCY_BP = 10000; // 100%
    public static final int BUTTERFLY_REPRODUCTION_RATE_BP = 1000; // 10%

    // Cold-blooded Intervals
    public static final int COLD_BLOODED_MOVE_INTERVAL = 2;
    public static final int COLD_BLOODED_FEED_INTERVAL = 3;
    public static final int COLD_BLOODED_REPRO_INTERVAL = 4;

    // World Initialization Probability (0-100)
    public static final int DEFAULT_PREDATOR_PRESENCE_CHANCE = 20;
    public static final int DEFAULT_HERBIVORE_PRESENCE_CHANCE = 50;
    
    private SimulationConstants() {
    }
}
