package com.island.content.plants;

import static com.island.config.SimulationConstants.PLANT_GROWTH_RATE;
import static com.island.config.SimulationConstants.PLANT_INITIAL_BIOMASS_FACTOR;

import com.island.content.Organism;
import com.island.content.SpeciesKey;
import com.island.util.RandomUtils;

/**
 * Base class for plants. 
 * Plants are unique as they represent the total biomass of their type in a cell.
 * They don't consume energy; they produce it through growth.
 */
public abstract class Plant extends Organism {
    protected final String typeName;
    protected final SpeciesKey speciesKey;
    protected double biomass;
    protected final double maxBiomass;

    protected Plant(String typeName, SpeciesKey speciesKey, double maxBiomass) {
        super(1.0, 0, PLANT_INITIAL_BIOMASS_FACTOR); 
        this.typeName = typeName;
        this.speciesKey = speciesKey;
        this.maxBiomass = maxBiomass;
        this.biomass = maxBiomass * PLANT_INITIAL_BIOMASS_FACTOR; 
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
        return 100.0; // Plants always have full energy
    }

    public double getBiomass() {
        return biomass;
    }

    public void grow() {
        // Growth logic centralized around fixed rate for stability
        biomass = Math.min(maxBiomass, biomass + (maxBiomass * PLANT_GROWTH_RATE));
    }

    public void checkState() {
        ageOneTick();
        grow();
    }

    public double consumeBiomass(double amount) {
        double actualEaten = Math.min(biomass, amount);
        biomass -= actualEaten;
        return actualEaten;
    }

    @Override
    public void consumeEnergy(double amount) {
        // Plants don't consume energy
    }
}
