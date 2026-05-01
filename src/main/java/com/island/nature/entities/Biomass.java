package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.engine.SimulationNode;
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

    protected Biomass(Configuration config, String typeName, SpeciesKey speciesKey, long maxBiomass, int speed) {
        super(config, 1L, 0, config.getPlantInitialBiomassBP() / 100); 
        this.typeName = typeName;
        this.speciesKey = speciesKey;
        this.maxBiomass = maxBiomass;
        this.biomass = (maxBiomass * config.getPlantInitialBiomassBP()) / config.getScale10K(); 
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

    public void tick(SimulationNode<Organism> node) {
        grow(node, 1.0);
    }

    public void grow(SimulationNode<Organism> node, double growthModifier) {
        long old = biomass;
        long growth = (maxBiomass * config.getPlantGrowthRateBP()) / config.getScale10K();
        growth = (long) (growth * growthModifier);
        biomass = Math.min(maxBiomass, biomass + growth);
        reportChange(node, biomass - old);
    }

    public long consumeBiomass(long amount, SimulationNode<Organism> node) {
        long actualEaten = Math.min(biomass, amount);
        biomass -= actualEaten;
        reportChange(node, -actualEaten);
        return actualEaten;
    }

    public void addBiomass(long amount, SimulationNode<Organism> node) {
        long old = biomass;
        if (maxBiomass > 0) {
            this.biomass = Math.min(maxBiomass, this.biomass + amount);
        } else {
            this.biomass += amount;
        }
        reportChange(node, biomass - old);
    }

    private void reportChange(SimulationNode<Organism> node, long delta) {
        if (delta != 0 && node.getWorld() instanceof NatureWorld nw) {
            nw.getStatisticsService().registerBiomassChange(speciesKey, delta);
        }
    }

    @Override
    public void consumeEnergy(long amount) {
        // Biomass doesn't consume energy in the traditional sense
    }
}
