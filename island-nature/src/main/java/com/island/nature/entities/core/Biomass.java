package com.island.nature.entities.core;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.nature.entities.registry.NatureComponentFactory;
import lombok.Getter;
import lombok.Setter;
import com.island.nature.entities.domain.NatureWorld;

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

    protected Biomass(Configuration config, ComponentRegistry registry, String typeName, SpeciesKey speciesKey, long maxBiomass, int speed) {
        super(config, registry, 1L, 0, config.getPlantInitialBiomassBP() / 100); 
        this.typeName = typeName;
        this.speciesKey = speciesKey;
        this.maxBiomass = maxBiomass;
        this.biomass = (maxBiomass * config.getPlantInitialBiomassBP()) / config.getScale10K(); 
        setSpeed(speed);
        
        // Use factory for all nature-specific components
        new NatureComponentFactory().createBiomassComponents(this).forEach(this::addComponent);
    }

    @Override
    public SpeciesKey getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public int getEnergyPercentage() {
        return 100; // Biomass always has full energy for logic purposes
    }

    public long consumeBiomass(long amount, Cell cell) {
        long actualEaten = Math.min(biomass, amount);
        biomass -= actualEaten;
        reportChange(cell, -actualEaten);
        return actualEaten;
    }

    public void addBiomassAmount(long amount, Cell cell) {
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
            NatureWorld nw = (NatureWorld) cell.getWorld();
            nw.getStatisticsService().registerBiomassChange(speciesKey, delta);
        }
    }

    @Override
    public void consumeEnergy(long amount) {
        // Biomass doesn't consume energy in the traditional sense
    }
}
