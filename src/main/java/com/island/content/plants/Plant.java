package com.island.content.plants;

import com.island.content.Organism;
import com.island.content.Reproducible;
import com.island.util.RandomUtils;
import static com.island.config.SimulationConstants.*;

/**
 * Base class for plants. 
 * Plants are unique as they represent the total biomass of their type in a cell.
 * They don't consume energy; they produce it through growth.
 */
public abstract class Plant extends Organism implements Reproducible<Plant> {
    protected double biomass;
    protected final double maxBiomass;

    protected Plant(double maxBiomass) {
        super(1.0, 0, PLANT_INITIAL_BIOMASS_FACTOR); 
        this.maxBiomass = maxBiomass;
        this.biomass = maxBiomass * PLANT_INITIAL_BIOMASS_FACTOR; 
    }

    @Override
    public double getEnergyPercentage() {
        return 100.0; // Plants always have full energy
    }

    public double getBiomass() {
        return biomass;
    }

    public double getMaxBiomass() {
        return maxBiomass;
    }

    public void addBiomass(double amount) {
        biomass = Math.min(maxBiomass, biomass + amount);
    }

    public double consumeBiomass(double amount) {
        double actual = Math.min(biomass, amount);
        biomass -= actual;
        // Plant "dies" only if biomass reaches zero, but usually it grows back
        return actual;
    }

    @Override
    public boolean isAlive() {
        return true; // The root system is immortal in this model
    }

    /**
     * Growth logic: increases biomass by percentage of maximum biomass.
     */
    public void grow() {
        double growthRate = PLANT_GROWTH_RATE_MIN 
            + (RandomUtils.nextDouble() * (PLANT_GROWTH_RATE_MAX - PLANT_GROWTH_RATE_MIN));
        biomass = Math.min(maxBiomass, biomass + (maxBiomass * growthRate));
    }

    @Override
    public void checkState() {
        ageOneTick();
        grow();
    }

    @Override
    public void consumeEnergy(double amount) {
        // Plants don't consume energy
    }

    @Override
    public Plant reproduce() {
        return null; // Plants "reproduce" through growth/mass increase in this model
    }
}
