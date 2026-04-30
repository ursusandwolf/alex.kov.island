package com.island.engine;

import java.util.Map;

/**
 * Domain-agnostic snapshot of the simulation world state.
 */
public interface WorldSnapshot {
    int getTickCount();

    int getWidth();

    int getHeight();

    int getTotalOrganismCount();

    double getGlobalSatiety();

    int getStarvingCount();
    
    /**
     * Maps species code to total count.
     */
    Map<String, Integer> getSpeciesCounts();
    
    /**
     * Maps species code to death count for a specific cause.
     */
    Map<String, Integer> getDeathStatsBySpecies(String causeCode);
    
    /**
     * Total death count for a specific cause.
     */
    int getTotalDeathCount(String causeCode);

    /**
     * Gets snapshot of a specific node.
     */
    NodeSnapshot getNodeSnapshot(int x, int y);
}
