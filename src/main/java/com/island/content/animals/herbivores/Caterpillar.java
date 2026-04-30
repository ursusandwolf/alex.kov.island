package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.content.SwarmOrganism;
import com.island.model.Cell;
import java.util.List;

/**
 * Generalized Caterpillar using SwarmOrganism (LOD 1).
 */
public class Caterpillar extends SwarmOrganism {
    private final double[] sleepBuckets = new double[20];

    public Caterpillar(double maxBiomass, int speed) {
        super("Caterpillar", SpeciesKey.CATERPILLAR, maxBiomass, speed, 40, 
                CATERPILLAR_METABOLISM_RATE, 0); // Reproduction handled by Butterfly
        ageBuckets[0] = maxBiomass;
        updateTotalBiomass();
    }

    @Override
    protected void processFeeding(Cell cell) {
        double totalActive = 0;
        for (double s : ageBuckets) {
            totalActive += s;
        }

        double appetite = totalActive * 0.10; 
        if (appetite > 0) {
            List<Biomass> availablePlants = cell.getBiomassContainers();
            for (Biomass p : availablePlants) {
                if (p != this && p.isAlive() && !(p instanceof Butterfly)) {
                    double eaten = p.consumeBiomass(appetite / CATERPILLAR_FEED_EFFICIENCY, cell);
                    ageBuckets[0] += (eaten * CATERPILLAR_FEED_EFFICIENCY);
                    appetite -= eaten;
                    if (appetite <= 0) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void advanceAge(Cell cell) {
        // Handle metamorphosis
        double maturing = ageBuckets[ageBuckets.length - 1];
        
        // Emerging from sleep to Butterfly
        double emerging = sleepBuckets[sleepBuckets.length - 1];
        if (emerging > 0) {
            Butterfly b = (Butterfly) cell.getBiomass(SpeciesKey.BUTTERFLY);
            if (b == null) {
                b = new Butterfly(0, this.getSpeed());
                cell.addBiomass(b);
            }
            b.spawn(emerging);
        }

        // Shift sleep buckets
        for (int i = sleepBuckets.length - 1; i > 0; i--) {
            sleepBuckets[i] = sleepBuckets[i - 1];
        }
        sleepBuckets[0] = maturing * 0.5; // 50% go to sleep

        // Standard age advance
        for (int i = ageBuckets.length - 1; i > 0; i--) {
            ageBuckets[i] = ageBuckets[i - 1];
        }
        ageBuckets[0] = maturing * 0.5; // 50% reset cycle
    }

    @Override
    protected void processReproduction(Cell cell) {
        // Handled by Butterfly
    }

    @Override
    protected void updateTotalBiomass() {
        double total = 0;
        for (double b : ageBuckets) {
            total += b;
        }
        for (double b : sleepBuckets) {
            total += b;
        }
        this.biomass = total;
    }

    @Override
    public boolean isHibernating() {
        double sleepTotal = 0;
        for (double b : sleepBuckets) {
            sleepTotal += b;
        }
        return sleepTotal > (biomass * 0.1);
    }
}
