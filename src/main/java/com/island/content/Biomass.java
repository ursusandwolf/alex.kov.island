package com.island.content;

import static com.island.config.SimulationConstants.PLANT_GROWTH_RATE;
import static com.island.config.SimulationConstants.PLANT_INITIAL_BIOMASS_FACTOR;

import com.island.util.RandomUtils;
import com.island.model.Cell;

/**
 * Base class for all biomass-based organisms (Plants, Insects).
 * Represents the total biomass of their type in a cell.
 */
public abstract class Biomass extends Organism {
    protected final String typeName;
    protected final SpeciesKey speciesKey;
    protected double biomass;
    protected final double maxBiomass;
    protected final int speed;

    protected Biomass(String typeName, SpeciesKey speciesKey, double maxBiomass, int speed) {
        super(1.0, 0, PLANT_INITIAL_BIOMASS_FACTOR); 
        this.typeName = typeName;
        this.speciesKey = speciesKey;
        this.maxBiomass = maxBiomass;
        this.biomass = maxBiomass * PLANT_INITIAL_BIOMASS_FACTOR; 
        this.speed = speed;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public SpeciesKey getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public double getEnergyPercentage() {
        return 100.0; // Biomass always has full energy for logic purposes
    }

    public double getBiomass() {
        return biomass;
    }

    public void setBiomass(double biomass) {
        this.biomass = Math.max(0, Math.min(maxBiomass, biomass));
    }

    public int getSpeed() {
        return speed;
    }

    public void tick(Cell cell) {
        grow();
    }

    public void grow() {
        // Growth logic centralized around fixed rate for stability
        biomass = Math.min(maxBiomass, biomass + (maxBiomass * PLANT_GROWTH_RATE));
    }

    public double consumeBiomass(double amount) {
        double actualEaten = Math.min(biomass, amount);
        biomass -= actualEaten;
        return actualEaten;
    }

    public void addBiomass(double amount) {
        this.biomass = Math.min(maxBiomass, this.biomass + amount);
    }

    @Override
    public void consumeEnergy(double amount) {
        // Biomass doesn't consume energy in the traditional sense
    }
}
