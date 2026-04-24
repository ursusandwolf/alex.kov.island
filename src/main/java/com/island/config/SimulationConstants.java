package com.island.config;

public final class SimulationConstants {
    // Energy Costs
    public static final double BASE_MOVE_COST_PERCENT = 0.05;
    public static final double SPEED_MOVE_COST_STEP_PERCENT = 0.01;
    
    public static final double REPRODUCTION_COST_PERCENT = 0.30;
    
    public static final double BASE_HUNT_COST_PERCENT = 0.02;
    public static final double PREDATOR_SPEED_HUNT_COST_STEP_PERCENT = 0.01;
    public static final double PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT = 0.02;
    
    public static final double BASE_METABOLISM_PERCENT = 0.10;
    
    // Thresholds
    public static final double ACTION_MIN_ENERGY_PERCENT = 30.0;
    
    private SimulationConstants() {}
}
