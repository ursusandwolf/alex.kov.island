package com.island.nature.entities.core;

import com.island.engine.ecs.ComponentRegistry;

/**
 * A generic biomass implementation that uses AnimalType for its properties.
 */
public class GenericBiomass extends Biomass {
    public GenericBiomass(AnimalType type, ComponentRegistry registry) {
        super(type.getConfig(), registry, type.getTypeName(), type.getSpeciesKey(), type.getWeight() * type.getMaxPerCell(), type.getSpeed());
    }
}