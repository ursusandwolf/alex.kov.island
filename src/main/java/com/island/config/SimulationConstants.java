package com.island.config;

public final class SimulationConstants {
    // Energy Costs
    public static double BASE_MOVE_COST_PERCENT = 0.05;
    public static double SPEED_MOVE_COST_STEP_PERCENT = 0.01;
    
    public static double REPRODUCTION_COST_PERCENT = 0.15; 
    
    public static double BASE_HUNT_COST_PERCENT = 0.01; 
    public static double PREDATOR_SPEED_HUNT_COST_STEP_PERCENT = 0.005; 
    public static double PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT = 0.05; 
    
    public static double BASE_METABOLISM_PERCENT = 0.015; 
    public static double HERBIVORE_METABOLISM_MODIFIER = 0.80; 

    // Hunt Fatigue Constants
    public static int HUNT_FATIGUE_THRESHOLD = 5; 
    public static double HUNT_FATIGUE_COST_MULTIPLIER = 1.3; 
    
    // Thresholds
    public static double ACTION_MIN_ENERGY_PERCENT = 15.0; 
    public static double REPRODUCTION_MIN_ENERGY_PERCENT = 70.0;
    public static double BABY_INITIAL_ENERGY_PERCENT = 50.0; 
    public static double DEATH_EPSILON = 0.00001;
    public static double ESCAPE_ENERGY_COST_PERCENT = 0.05;

    // Red Book / Endangered Protection
    public static double ENDANGERED_POPULATION_THRESHOLD = 0.05; 
    public static double ENDANGERED_REPRO_BONUS_PERCENT = 20.0; 

    // Reproduction Scaling
    public static int HERBIVORE_OFFSPRING_BONUS = 1; 

    // Plants logic
    public static double PLANT_INITIAL_BIOMASS_FACTOR = 0.5; 
    public static double PLANT_GROWTH_RATE_MIN = 0.10; 
    public static double PLANT_GROWTH_RATE_MAX = 0.10; 
    
    // Caterpillar Pendulum Constants
    public static double CATERPILLAR_METABOLISM_RATE = 0.05;
    public static double CATERPILLAR_FERTILIZER_EFFICIENCY = 1.0; 
    public static double CATERPILLAR_FEED_EFFICIENCY = 1.0; 
    
    private SimulationConstants() {
    }
}
