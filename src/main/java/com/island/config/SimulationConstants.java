package com.island.config;

public final class SimulationConstants {
    // Energy Costs
    public static final double SPEED_MOVE_COST_STEP_PERCENT = 0.01;
    
    public static final double PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT = 0.05; 
    
    public static final double BASE_METABOLISM_PERCENT = 0.015; 
    public static final double HERBIVORE_METABOLISM_MODIFIER = 0.80; 

    // Hunt Fatigue Constants
    public static final int HUNT_FATIGUE_THRESHOLD = 5; 
    public static final double HUNT_FATIGUE_COST_MULTIPLIER = 1.3; 
    
    // Thresholds
    public static final double DEATH_EPSILON = 0.00001;

    // Red Book / Endangered Protection
    public static final double ENDANGERED_POPULATION_THRESHOLD = 0.05; 
    public static final double ENDANGERED_REPRO_BONUS_PERCENT = 20.0; 

    // Reproduction Scaling
    public static final int HERBIVORE_OFFSPRING_BONUS = 1; 

    // Plants logic
    public static final double PLANT_INITIAL_BIOMASS_FACTOR = 0.5; 
    public static final double PLANT_GROWTH_RATE = 0.10; 

    // Caterpillar Pendulum Constants
    public static final double CATERPILLAR_METABOLISM_RATE = 0.05;
    public static final double CATERPILLAR_FEED_EFFICIENCY = 1.0; 
    
    private SimulationConstants() {
    }
}
