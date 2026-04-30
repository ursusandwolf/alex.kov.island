package com.island.config;

public final class SimulationConstants {
    // Energy Costs (Perfected middle ground)
    public static final double SPEED_MOVE_COST_STEP_PERCENT = 0.01; 
    public static final double PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT = 0.05; 
    public static final double BASE_METABOLISM_PERCENT = 0.015; // 1.5% is sustainable
    public static final double HERBIVORE_METABOLISM_MODIFIER = 0.70; 
    public static final double REPTILE_METABOLISM_MODIFIER = 0.40; 

    // Hunting Logic
    public static final double HUNT_STRIKE_COST_PREY_WEIGHT_FRACTION = 0.1;
    public static final double HUNT_STRIKE_COST_MAX_ENERGY_CAP = 0.005;
    public static final double HUNT_ROI_THRESHOLD = 1.1;

    // Wolf Pack Hunting
    public static final int WOLF_PACK_MIN_SIZE = 3;
    public static final int WOLF_PACK_MAX_BONUS_PERCENT = 30;
    public static final int WOLF_PACK_BEAR_HUNT_MAX_CHANCE = 30;

    // Hunt Fatigue Constants
    public static final int HUNT_FATIGUE_THRESHOLD = 5; 
    public static final double HUNT_FATIGUE_COST_MULTIPLIER = 1.3; 
    
    // Feeding Penalties & Bonuses
    public static final double PREDATOR_FAIL_HUNT_PENALTY_PERCENT = 0.05;
    public static final double HERBIVORE_FAIL_FEED_PENALTY_PERCENT = 0.03;
    public static final double OVERPOPULATION_HUNT_BONUS = 15.0;

    // LOD Sampling
    public static final int FEEDING_LOD_LIMIT = 500;
    public static final int REPRODUCTION_LOD_LIMIT = 30;

    // Thresholds
    public static final double DEATH_EPSILON = 0.00001;
    public static final double STARVATION_THRESHOLD_PERCENT = 30.0;
    public static final double REPRODUCTION_MIN_ENERGY_PERCENT = 50.0; 

    // Red Book / Endangered Protection
    public static final double ENDANGERED_POPULATION_THRESHOLD = 0.05; 
    public static final double ENDANGERED_REPRO_BONUS_PERCENT = 20.0; 
    public static final int ENDANGERED_SPEED_BONUS = 2;
    public static final double ENDANGERED_MAX_HIDE_CHANCE = 0.60;
    public static final double ENDANGERED_MIN_HIDE_CHANCE = 0.30;

    // Reproduction Scaling
    public static final int HERBIVORE_OFFSPRING_BONUS = 1; 

    // Plants & Biomass logic
    public static final double PLANT_INITIAL_BIOMASS_FACTOR = 0.8; // High start
    public static final double PLANT_GROWTH_RATE = 0.80; // High growth to support life
    public static final double BIOMASS_MOVE_CHUNK_FRACTION = 0.25;

    // Caterpillar Pendulum Constants
    public static final double CATERPILLAR_METABOLISM_RATE = 0.05;
    public static final double CATERPILLAR_FEED_EFFICIENCY = 1.0; 
    public static final double BUTTERFLY_REPRODUCTION_RATE = 0.10; 

    // Cold-blooded Intervals
    public static final int COLD_BLOODED_MOVE_INTERVAL = 2;
    public static final int COLD_BLOODED_FEED_INTERVAL = 3;
    public static final int COLD_BLOODED_REPRO_INTERVAL = 4;

    // World Initialization Probability
    public static final double DEFAULT_PREDATOR_PRESENCE_PROB = 0.2;
    public static final double DEFAULT_HERBIVORE_PRESENCE_PROB = 0.5; // Very high herbivore start
    
    private SimulationConstants() {
    }
}
