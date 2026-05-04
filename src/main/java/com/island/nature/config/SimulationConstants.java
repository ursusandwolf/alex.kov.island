package com.island.nature.config;

/**
 * Registry for default simulation constants.
 * @deprecated Use {@link Configuration} instead for multi-instance support.
 */
@Deprecated
public final class SimulationConstants {
    private static final Configuration DEFAULT = new Configuration();

    public static final long SCALE_1M = DEFAULT.getScale1M();
    public static final int SCALE_10K = DEFAULT.getScale10K();
    public static final int PERCENT_100 = 100;

    public static final int SPEED_MOVE_COST_STEP_BP = DEFAULT.getSpeedMoveCostStepBP(); 
    public static final int PREY_RELATIVE_SPEED_HUNT_COST_STEP_BP = DEFAULT.getPreyRelativeSpeedHuntCostStepBP(); 
    public static final int BASE_METABOLISM_BP = DEFAULT.getBaseMetabolismBP(); 
    public static final int HERBIVORE_METABOLISM_MODIFIER_BP = DEFAULT.getHerbivoreMetabolismModifierBP(); 
    public static final int REPTILE_METABOLISM_MODIFIER_BP = DEFAULT.getReptileMetabolismModifierBP(); 

    public static final int HUNT_STRIKE_COST_PREY_WEIGHT_BP = DEFAULT.getHuntStrikeCostPreyWeightBP(); 
    public static final int HUNT_STRIKE_COST_MAX_ENERGY_CAP_BP = DEFAULT.getHuntStrikeCostMaxEnergyCapBP(); 
    public static final int HUNT_ROI_THRESHOLD_BP = DEFAULT.getHuntRoiThresholdBP(); 

    public static final int WOLF_PACK_MIN_SIZE = DEFAULT.getWolfPackMinSize();
    public static final int WOLF_PACK_MAX_BONUS_PERCENT = DEFAULT.getWolfPackMaxBonusPercent();
    public static final int WOLF_PACK_BEAR_HUNT_MAX_CHANCE_PERCENT = DEFAULT.getWolfPackBearHuntMaxChancePercent();

    public static final int HUNT_FATIGUE_THRESHOLD = DEFAULT.getHuntFatigueThreshold(); 
    public static final int HUNT_FATIGUE_COST_MODIFIER_BP = DEFAULT.getHuntFatigueCostModifierBP(); 
    
    public static final int PREDATOR_FAIL_HUNT_PENALTY_BP = DEFAULT.getPredatorFailHuntPenaltyBP(); 
    public static final int HERBIVORE_FAIL_FEED_PENALTY_BP = DEFAULT.getHerbivoreFailFeedPenaltyBP(); 
    public static final int OVERPOPULATION_HUNT_BONUS_PERCENT = DEFAULT.getOverpopulationHuntBonusPercent();

    public static final int FEEDING_LOD_LIMIT = DEFAULT.getFeedingLodLimit();
    public static final int REPRODUCTION_LOD_LIMIT = DEFAULT.getReproductionLodLimit();
    public static final int MOVEMENT_LOD_LIMIT = DEFAULT.getMovementLodLimit();

    public static final long DEATH_ENERGY_THRESHOLD = DEFAULT.getDeathEnergyThreshold();
    public static final int HUNGER_THRESHOLD_PERCENT = DEFAULT.getHungerThresholdPercent();
    public static final int REPRODUCTION_MIN_ENERGY_PERCENT = DEFAULT.getReproductionMinEnergyPercent();
    public static final int ENDANGERED_POPULATION_THRESHOLD_BP = DEFAULT.getEndangeredPopulationThresholdBP(); 
    public static final int ENDANGERED_REPRO_BONUS_BP = DEFAULT.getEndangeredReproBonusBP(); 
    public static final int ENDANGERED_SPEED_BONUS = DEFAULT.getEndangeredSpeedBonus();
    public static final int ENDANGERED_MAX_HIDE_CHANCE_PERCENT = DEFAULT.getEndangeredMaxHideChancePercent();
    public static final int ENDANGERED_MIN_HIDE_CHANCE_PERCENT = DEFAULT.getEndangeredMinHideChancePercent();

    public static final int HERBIVORE_OFFSPRING_BONUS = DEFAULT.getHerbivoreOffspringBonus(); 

    public static final int PLANT_INITIAL_BIOMASS_BP = DEFAULT.getPlantInitialBiomassBP(); 
    public static final int PLANT_GROWTH_RATE_BP = DEFAULT.getPlantGrowthRateBP(); 
    public static final int BIOMASS_MOVE_CHUNK_BP = DEFAULT.getBiomassMoveChunkBP(); 

    public static final int CATERPILLAR_METABOLISM_RATE_BP = DEFAULT.getCaterpillarMetabolismRateBP(); 
    public static final int CATERPILLAR_FEED_EFFICIENCY_BP = DEFAULT.getCaterpillarFeedEfficiencyBP(); 
    public static final int BUTTERFLY_REPRODUCTION_RATE_BP = DEFAULT.getButterflyReproductionRateBP(); 

    public static final int COLD_BLOODED_MOVE_INTERVAL = DEFAULT.getColdBloodedMoveInterval();
    public static final int COLD_BLOODED_FEED_INTERVAL = DEFAULT.getColdBloodedFeedInterval();
    public static final int COLD_BLOODED_REPRO_INTERVAL = DEFAULT.getColdBloodedReproInterval();
    public static final int HIBERNATION_METABOLISM_MODIFIER_BP = DEFAULT.getHibernationMetabolismModifierBP();

    public static final int DEFAULT_PREDATOR_PRESENCE_CHANCE = DEFAULT.getDefaultPredatorPresenceChance();
    public static final int DEFAULT_HERBIVORE_PRESENCE_CHANCE = DEFAULT.getDefaultHerbivorePresenceChance();
    
    private SimulationConstants() { }
}
