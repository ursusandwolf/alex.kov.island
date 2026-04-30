package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY_BP;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE_BP;
import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.content.SwarmOrganism;
import com.island.model.Cell;
import java.util.List;

/**
 * Generalized Caterpillar using SwarmOrganism (LOD 1) with integer arithmetic.
 */
public class Caterpillar extends SwarmOrganism {
    private final long[] sleepBuckets = new long[20];

    public Caterpillar(long maxBiomass, int speed) {
        super("Caterpillar", SpeciesKey.CATERPILLAR, maxBiomass, speed, 40, 
                CATERPILLAR_METABOLISM_RATE_BP, 0); // Reproduction handled by Butterfly
        ageBuckets[0] = maxBiomass;
        updateTotalBiomass();
    }

    @Override
    protected void processFeeding(Cell cell) {
        long totalActive = 0;
        for (long s : ageBuckets) {
            totalActive += s;
        }

        long appetite = totalActive / 10; // 10%
        if (appetite > 0) {
            List<Biomass> availablePlants = cell.getBiomassContainers();
            for (Biomass p : availablePlants) {
                if (p != this && p.isAlive() && !(p instanceof Butterfly)) {
                    // appetite = consumed * efficiency / SCALE_10K
                    // consumed = appetite * SCALE_10K / efficiency
                    long consumed = (appetite * SCALE_10K) / CATERPILLAR_FEED_EFFICIENCY_BP;
                    long actualEaten = p.consumeBiomass(consumed, cell);
                    long energyGain = (actualEaten * CATERPILLAR_FEED_EFFICIENCY_BP) / SCALE_10K;
                    ageBuckets[0] += energyGain;
                    appetite -= energyGain;
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
        long maturing = ageBuckets[ageBuckets.length - 1];
        
        // Emerging from sleep to Butterfly
        long emerging = sleepBuckets[sleepBuckets.length - 1];
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
        sleepBuckets[0] = maturing / 2; // 50% go to sleep

        // Standard age advance
        for (int i = ageBuckets.length - 1; i > 0; i--) {
            ageBuckets[i] = ageBuckets[i - 1];
        }
        ageBuckets[0] = maturing / 2; // 50% reset cycle
    }

    @Override
    protected void processReproduction(Cell cell) {
        // Handled by Butterfly
    }

    @Override
    protected void updateTotalBiomass() {
        long total = 0;
        for (long b : ageBuckets) {
            total += b;
        }
        for (long b : sleepBuckets) {
            total += b;
        }
        this.biomass = total;
    }

    @Override
    public boolean isHibernating() {
        long sleepTotal = 0;
        for (long b : sleepBuckets) {
            sleepTotal += b;
        }
        return sleepTotal > (biomass / 10);
    }
}
