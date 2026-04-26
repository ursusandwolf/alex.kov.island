package com.island.config;

public final class SimulationConstants {
    // Energy Costs
    public static double BASE_MOVE_COST_PERCENT = 0.05;
    public static double SPEED_MOVE_COST_STEP_PERCENT = 0.01;
    
    public static double REPRODUCTION_COST_PERCENT = 0.30;
    
    public static double BASE_HUNT_COST_PERCENT = 0.01; 
    public static double PREDATOR_SPEED_HUNT_COST_STEP_PERCENT = 0.005; 
    public static double PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT = 0.05; // Increased penalty for speed diff
    
    public static double BASE_METABOLISM_PERCENT = 0.015; // Adjusted to 1.5%
    
    // Metabolism Step Modifiers
    public static double METABOLISM_MODIFIER_TINY = 1.20; 
    public static double METABOLISM_MODIFIER_MEDIUM = 1.00; 
    public static double METABOLISM_MODIFIER_LARGE = 0.80; 
    public static double HERBIVORE_METABOLISM_MODIFIER = 0.80; 

    // Hunt Fatigue Constants
    public static int HUNT_FATIGUE_THRESHOLD = 5; // Reduced from 10: Predators tire 2x faster
    public static double HUNT_FATIGUE_COST_MULTIPLIER = 1.3; // +30% cost per block 
    
    // Thresholds
    public static double ACTION_MIN_ENERGY_PERCENT = 15.0; 
    public static double REPRODUCTION_MIN_ENERGY_PERCENT = 60.0; 
    public static double BABY_INITIAL_ENERGY_PERCENT = 50.0; // Babies start stronger
    public static double DEATH_EPSILON = 0.00001;
    public static double ESCAPE_ENERGY_COST_PERCENT = 0.05;

    // Red Book / Endangered Protection
    public static double ENDANGERED_POPULATION_THRESHOLD = 0.05; 
    public static double ENDANGERED_REPRO_BONUS_PERCENT = 20.0; 

    // Reproduction Scaling (Increased to counteract death rate)
    public static int OFFSPRING_SMALL_ANIMAL = 4; 
    public static int OFFSPRING_LARGE_ANIMAL = 2; 
    public static int OFFSPRING_INSECT = 15; 
    public static double WEIGHT_THRESHOLD_SMALL = 6.0;
    public static int HERBIVORE_OFFSPRING_BONUS = 2; 

    // Plants logic (Aggressive growth to support large population)
    public static double PLANT_INITIAL_BIOMASS_FACTOR = 0.5; 
    public static double PLANT_GROWTH_RATE_MIN = 0.40; 
    public static double PLANT_GROWTH_RATE_MAX = 0.80; 

    public static final double GRASS_WEIGHT = 1.0;
    public static final int GRASS_MAX_COUNT = 500; 
    public static final double CABBAGE_WEIGHT = 2.0;
    public static final int CABBAGE_MAX_COUNT = 250; 
    
    // Caterpillar (Smart Biomass) Pendulum Constants
    public static double CATERPILLAR_METABOLISM_RATE = 0.05;
    public static double CATERPILLAR_FERTILIZER_EFFICIENCY = 1.0; 
    public static double CATERPILLAR_FEED_EFFICIENCY = 1.0; 
    
    private SimulationConstants() {}
}
