package com.island.config;

public final class SimulationConstants {
    // Energy Costs (Non-final for stability tuning in tests)
    public static double BASE_MOVE_COST_PERCENT = 0.05;
    public static double SPEED_MOVE_COST_STEP_PERCENT = 0.01;
    
    public static double REPRODUCTION_COST_PERCENT = 0.30;
    
    public static double BASE_HUNT_COST_PERCENT = 0.02;
    public static double PREDATOR_SPEED_HUNT_COST_STEP_PERCENT = 0.01;
    public static double PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT = 0.02;
    
    public static double BASE_METABOLISM_PERCENT = 0.07;
    
    // Thresholds
    public static double ACTION_MIN_ENERGY_PERCENT = 30.0;
    public static double REPRODUCTION_MIN_ENERGY_PERCENT = 70.0;
    public static double BABY_INITIAL_ENERGY_PERCENT = 30.0;
    public static double DEATH_EPSILON = 0.00001;
    public static double ESCAPE_ENERGY_COST_PERCENT = 0.05;

    // Reproduction Scaling
    public static int OFFSPRING_SMALL_ANIMAL = 2;
    public static int OFFSPRING_LARGE_ANIMAL = 1;
    public static int OFFSPRING_INSECT = 4;
    public static double WEIGHT_THRESHOLD_SMALL = 6.0;
    public static int HERBIVORE_OFFSPRING_BONUS = 1;

    // Plants logic
    public static double PLANT_INITIAL_BIOMASS_FACTOR = 0.1;
    public static double PLANT_GROWTH_RATE_MIN = 0.05;
    public static double PLANT_GROWTH_RATE_MAX = 0.10;

    public static final double GRASS_WEIGHT = 1.0;
    public static final int GRASS_MAX_COUNT = 200;
    public static final double CABBAGE_WEIGHT = 2.0;
    public static final int CABBAGE_MAX_COUNT = 100;
    
    private SimulationConstants() {}
}
