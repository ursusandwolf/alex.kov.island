package com.island.nature.model;

import java.util.HashMap;
import java.util.Map;
import com.island.engine.model.NodeSnapshot;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;

/**
 * Island-specific implementation of NodeSnapshot using integer arithmetic for sorting.
 */
public class CellSnapshot implements NodeSnapshot {
    private final String coordinates;
    private final String topSpeciesCode;
    private final boolean isTopSpeciesPlant;
    private final boolean hasOrganisms;
    private final Map<String, Integer> entityCounts;

    public CellSnapshot(Cell cell) {
        this.coordinates = cell.getCoordinates();
        
        Map<SpeciesKey, Long> biomassMap = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        
        cell.forEachAnimal(a -> {
            if (a.isAlive()) {
                biomassMap.merge(a.getSpeciesKey(), a.getWeight(), Long::sum);
                counts.merge(a.getSpeciesKey().getCode(), 1, Integer::sum);
            }
        });
        for (Biomass b : cell.getBiomassContainers()) {
            if (b.isAlive() && b.getBiomass() > 0) {
                biomassMap.merge(b.getSpeciesKey(), b.getBiomass(), Long::sum);
                counts.merge(b.getSpeciesKey().getCode(), 1, Integer::sum);
            }
        }

        this.entityCounts = Map.copyOf(counts);
        this.hasOrganisms = !biomassMap.isEmpty();
        
        if (hasOrganisms && cell.getWorld() instanceof NatureWorld nw) {
            SpeciesKey top = null;
            long maxWeight = -1;
            for (Map.Entry<SpeciesKey, Long> entry : biomassMap.entrySet()) {
                if (entry.getValue() > maxWeight) {
                    maxWeight = entry.getValue();
                    top = entry.getKey();
                }
            }
            this.topSpeciesCode = (top != null) ? top.getCode() : null;
            this.isTopSpeciesPlant = top != null && nw.getRegistry().getAnyType(top)
                    .map(AnimalType::isPlant).orElse(false);
        } else {
            this.topSpeciesCode = null;
            this.isTopSpeciesPlant = false;
        }
    }

    @Override
    public String getCoordinates() {
        return coordinates;
    }

    @Override
    public String getTopSpeciesCode() {
        return topSpeciesCode;
    }

    @Override
    public boolean isTopSpeciesPlant() {
        return isTopSpeciesPlant;
    }

    @Override
    public boolean hasOrganisms() {
        return hasOrganisms;
    }
    
    @Override
    public Map<String, Integer> getEntityCounts() {
        return entityCounts;
    }
}