package com.island.content;

import static com.island.config.SimulationConstants.PLANT_GROWTH_RATE;
import static com.island.config.SimulationConstants.PLANT_INITIAL_BIOMASS_FACTOR;

import com.island.util.RandomUtils;
import com.island.model.Cell;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all biomass-based organisms (Plants, Insects).
 * Represents the total biomass of their type in a cell.
 */
@Getter
public abstract class Biomass extends Organism {
    protected final String typeName;
    protected final SpeciesKey speciesKey;
    @Setter
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
    public SpeciesKey getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public double getEnergyPercentage() {
        return 100.0; // Biomass always has full energy for logic purposes
    }

    public void tick(Cell cell) {
        grow(cell);
    }

    public void grow(Cell cell) {
        double old = biomass;
        biomass = Math.min(maxBiomass, biomass + (maxBiomass * PLANT_GROWTH_RATE));
        reportChange(cell, biomass - old);
    }

    public double consumeBiomass(double amount, Cell cell) {
        double actualEaten = Math.min(biomass, amount);
        biomass -= actualEaten;
        reportChange(cell, -actualEaten);
        return actualEaten;
    }

    public void addBiomass(double amount, Cell cell) {
        double old = biomass;
        if (maxBiomass > 0) {
            this.biomass = Math.min(maxBiomass, this.biomass + amount);
        } else {
            this.biomass += amount;
        }
        reportChange(cell, biomass - old);
    }

    private void reportChange(Cell cell, double delta) {
        if (delta != 0) {
            cell.getWorld().getStatisticsService().registerBiomassChange(speciesKey, delta);
        }
    }

    @Override
    public void consumeEnergy(double amount) {
        // Biomass doesn't consume energy in the traditional sense
    }
}
