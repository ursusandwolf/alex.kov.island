package com.island.content;

import com.island.engine.SimulationWorld;
import com.island.model.Cell;
import com.island.service.ProtectionService;
import com.island.service.StatisticsService;
import java.util.Map;

/**
 * Domain-specific extension of SimulationWorld for nature/island simulation.
 */
public interface NatureWorld extends SimulationWorld<Organism> {
    SpeciesRegistry getRegistry();

    StatisticsService getStatisticsService();

    ProtectionService getProtectionService();

    Map<SpeciesKey, Integer> getProtectionMap();

    Season getCurrentSeason();

    void reportDeath(SpeciesKey key, DeathCause cause);

    void onOrganismAdded(SpeciesKey key);

    void onOrganismRemoved(SpeciesKey key);

    void moveBiomassPartially(Biomass b, Cell from, Cell to, long amount);
}
