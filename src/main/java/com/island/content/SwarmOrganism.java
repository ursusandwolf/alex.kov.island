package com.island.content;

import com.island.model.Cell;
import static com.island.config.SimulationConstants.SCALE_10K;
import static com.island.config.SimulationConstants.SCALE_1M;
import lombok.Getter;

/**
 * Generalized LOD 1 (Aggregated) organism using integer-based arithmetic.
 * Represents a population of individuals of the same species in a cell.
 * Tracks population counts across age buckets (SCALE_1M).
 */
@Getter
public abstract class SwarmOrganism extends Biomass {
    protected final long[] ageBuckets;
    protected final int metabolismRateBP; // SCALE_10K
    protected final int reproductionRateBP; // SCALE_10K

    protected SwarmOrganism(String typeName, SpeciesKey speciesKey, long maxBiomass, 
                            int speed, int maxAge, int metabolismRateBP, int reproductionRateBP) {
        super(typeName, speciesKey, maxBiomass, speed);
        this.ageBuckets = new long[maxAge + 1];
        this.metabolismRateBP = metabolismRateBP;
        this.reproductionRateBP = reproductionRateBP;
    }

    @Override
    public void tick(Cell cell) {
        processLifecycle(cell);
    }

    protected void processLifecycle(Cell cell) {
        final long oldBiomass = getBiomass();
        
        // 1. Metabolism (Energy decay)
        applyMetabolism();

        // 2. Feeding (Handled by subclasses or generalized here)
        processFeeding(cell);

        // 3. Aging and Death
        advanceAge(cell);

        // 4. Reproduction
        processReproduction(cell);

        // 5. Sync total biomass
        updateTotalBiomass();
        reportChange(cell, getBiomass() - oldBiomass);
    }

    protected void applyMetabolism() {
        for (int i = 0; i < ageBuckets.length; i++) {
            ageBuckets[i] = (ageBuckets[i] * (SCALE_10K - metabolismRateBP)) / SCALE_10K;
        }
    }

    protected abstract void processFeeding(Cell cell);

    protected void advanceAge(Cell cell) {
        // Shift buckets
        for (int i = ageBuckets.length - 1; i > 0; i--) {
            ageBuckets[i] = ageBuckets[i - 1];
        }
        ageBuckets[0] = 0; // New offspring will be added here
    }

    protected abstract void processReproduction(Cell cell);

    protected void updateTotalBiomass() {
        long total = 0;
        for (long bucket : ageBuckets) {
            total += bucket;
        }
        setBiomass(total);
    }

    public void spawn(long amount) {
        if (amount > 0) {
            ageBuckets[0] += amount;
            updateTotalBiomass();
        }
    }

    @Override
    public long consumeBiomass(long amount, Cell cell) {
        long total = getBiomass();
        long actualEaten = Math.min(total, amount);
        if (actualEaten > 0 && total > 0) {
            // factor = (total - actualEaten) / total
            // To maintain precision: ageBuckets[i] = (ageBuckets[i] * (total - actualEaten)) / total
            long remaining = total - actualEaten;
            for (int i = 0; i < ageBuckets.length; i++) {
                ageBuckets[i] = (ageBuckets[i] * remaining) / total;
            }
            updateTotalBiomass();
        }
        return actualEaten;
    }

    protected void reportChange(Cell cell, long delta) {
        if (delta != 0) {
            cell.getWorld().getStatisticsService().registerBiomassChange(getSpeciesKey(), delta);
        }
    }
}
