package com.island.nature.model;

import lombok.RequiredArgsConstructor;
import com.island.engine.core.SimulationNode;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.registry.BiomassManager;

/**
 * Default implementation of BiomassManager.
 * Handles partial biomass movement between cells with proper locking.
 */
@RequiredArgsConstructor
public class DefaultBiomassManager implements BiomassManager {

    @Override
    public void moveBiomassPartially(Biomass b, Cell from, Cell to, long amount) {
        if (from == to || amount <= 0 || b.getBiomass() <= 0) {
            return;
        }

        // Lock ordering to prevent deadlocks
        Cell first = (from.getX() < to.getX() || (from.getX() == to.getX() && from.getY() < to.getY())) ? from : to;
        Cell second = (first == from) ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                long actualToMove = Math.min(b.getBiomass(), amount);
                if (to.addBiomass(b.getSpeciesKey(), actualToMove)) {
                    b.consumeBiomass(actualToMove, from);
                }
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }
}