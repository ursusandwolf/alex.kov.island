package com.island.content;

import static com.island.config.SimulationConstants.PLANT_GROWTH_RATE_BP;
import static com.island.config.SimulationConstants.PLANT_INITIAL_BIOMASS_BP;
import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.model.Cell;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all biomass-based organisms (Plants, Insects).
 * Represents the total biomass of their type in a cell using integer-based arithmetic.
 * Biomass is stored as long (SCALE_1M).
 */
@Getter
public abstract class Biomass extends Organism {
    protected final String typeName;
    protected final SpeciesKey speciesKey;
    @Setter
    protected long biomass;
    protected final long maxBiomass;
    protected final int speed;

    protected Biomass(String typeName, SpeciesKey speciesKey, long maxBiomass, int speed) {
        super(1L, 0, PLANT_INITIAL_BIOMASS_BP / 100); 
        this.typeName = typeName;
        this.speciesKey = speciesKey;
        this.maxBiomass = maxBiomass;
        this.biomass = (maxBiomass * PLANT_INITIAL_BIOMASS_BP) / SCALE_10K; 
        this.speed = speed;
    }

    @Override
    public SpeciesKey getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public int getEnergyPercentage() {
        return 100; // Biomass always has full energy for logic purposes
    }

    public void tick(Cell cell) {
        grow(cell);
    }

    public void grow(Cell cell) {
        long old = biomass;
        long growth = (maxBiomass * PLANT_GROWTH_RATE_BP) / SCALE_10K;
        biomass = Math.min(maxBiomass, biomass + growth);
        reportChange(cell, biomass - old);
    }

    public long consumeBiomass(long amount, Cell cell) {
        long actualEaten = Math.min(biomass, amount);
        biomass -= actualEaten;
        reportChange(cell, -actualEaten);
        return actualEaten;
    }

    public void addBiomass(long amount, Cell cell) {
        long old = biomass;
        if (maxBiomass > 0) {
            this.biomass = Math.min(maxBiomass, this.biomass + amount);
        } else {
            this.biomass += amount;
        }
        reportChange(cell, biomass - old);
    }

    private void reportChange(Cell cell, long delta) {
        if (delta != 0) {
            cell.getWorld().getStatisticsService().registerBiomassChange(speciesKey, delta);
        }
    }

    @Override
    public void consumeEnergy(long amount) {
        // Biomass doesn't consume energy in the traditional sense
    }
}
