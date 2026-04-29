package com.island.content;

import com.island.model.Cell;

/**
 * A generic biomass implementation that uses AnimalType for its properties.
 */
public class GenericBiomass extends Biomass {
    public GenericBiomass(AnimalType type) {
        super(type.getTypeName(), type.getSpeciesKey(), type.getMaxEnergy() * type.getMaxPerCell(), type.getSpeed());
    }
}
