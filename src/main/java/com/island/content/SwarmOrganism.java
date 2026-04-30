package com.island.content;

import com.island.model.Cell;
import java.util.Arrays;
import lombok.Getter;

/**
 * Generalized LOD 1 (Aggregated) organism.
 * Represents a population of individuals of the same species in a cell.
 * Tracks population counts across age buckets.
 */
@Getter
public abstract class SwarmOrganism extends Biomass {
    protected final double[] ageBuckets;
    protected final double metabolismRate;
    protected final double reproductionRate;

    protected SwarmOrganism(String typeName, SpeciesKey speciesKey, double maxBiomass, 
                            int speed, int maxAge, double metabolismRate, double reproductionRate) {
        super(typeName, speciesKey, maxBiomass, speed);
        this.ageBuckets = new double[maxAge + 1];
        this.metabolismRate = metabolismRate;
        this.reproductionRate = reproductionRate;
    }

    @Override
    public void tick(Cell cell) {
        processLifecycle(cell);
    }

    protected void processLifecycle(Cell cell) {
        final double oldBiomass = getBiomass();
        
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
            ageBuckets[i] *= (1.0 - metabolismRate);
        }
    }

    protected abstract void processFeeding(Cell cell);

    protected void advanceAge(Cell cell) {
        // Last bucket dies of old age
        double dying = ageBuckets[ageBuckets.length - 1];
        // Shift buckets
        for (int i = ageBuckets.length - 1; i > 0; i--) {
            ageBuckets[i] = ageBuckets[i - 1];
        }
        ageBuckets[0] = 0; // New offspring will be added here
    }

    protected abstract void processReproduction(Cell cell);

    protected void updateTotalBiomass() {
        double total = 0;
        for (double bucket : ageBuckets) {
            total += bucket;
        }
        setBiomass(total);
    }

    public void spawn(double amount) {
        if (amount > 0) {
            ageBuckets[0] += amount;
            updateTotalBiomass();
        }
    }

    @Override
    public double consumeBiomass(double amount, Cell cell) {
        double total = getBiomass();
        double actualEaten = Math.min(total, amount);
        if (actualEaten > 0) {
            double factor = (total - actualEaten) / total;
            for (int i = 0; i < ageBuckets.length; i++) {
                ageBuckets[i] *= factor;
            }
            updateTotalBiomass();
        }
        return actualEaten;
    }

    protected void reportChange(Cell cell, double delta) {
        if (delta != 0) {
            cell.getWorld().getStatisticsService().registerBiomassChange(getSpeciesKey(), delta);
        }
    }
}
