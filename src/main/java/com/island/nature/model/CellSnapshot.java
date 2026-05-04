package com.island.nature.model;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.SpeciesKey;
import com.island.engine.NodeSnapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * Island-specific implementation of NodeSnapshot using integer arithmetic for sorting.
 */
public class CellSnapshot implements NodeSnapshot {
    private final String coordinates;
    private final String topSpeciesCode;
    private final boolean isTopSpeciesPlant;
    private final boolean hasOrganisms;

    public CellSnapshot(Cell cell) {
        this.coordinates = cell.getCoordinates();
        
        Map<SpeciesKey, Long> biomassMap = new HashMap<>();
        cell.forEachAnimal(a -> {
            if (a.isAlive()) {
                biomassMap.merge(a.getSpeciesKey(), a.getWeight(), Long::sum);
            }
        });
        for (Biomass b : cell.getBiomassContainers()) {
            if (b.isAlive() && b.getBiomass() > 0) {
                biomassMap.merge(b.getSpeciesKey(), b.getBiomass(), Long::sum);
            }
        }

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
}
