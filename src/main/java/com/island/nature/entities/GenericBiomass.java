package com.island.nature.entities;

import com.island.nature.model.Cell;

/**
 * A generic biomass implementation that uses AnimalType for its properties.
 */
public class GenericBiomass extends Biomass {
    public GenericBiomass(AnimalType type) {
        super(type.getConfig(), type.getTypeName(), type.getSpeciesKey(), type.getWeight() * type.getMaxPerCell(), type.getSpeed());
    }
}
