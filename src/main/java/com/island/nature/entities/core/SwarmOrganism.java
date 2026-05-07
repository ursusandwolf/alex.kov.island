package com.island.nature.entities.core;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import lombok.Getter;
import com.island.nature.model.Cell;
import com.island.nature.entities.domain.NatureWorld;

@Getter
public abstract class SwarmOrganism extends Biomass {
    protected final long[] ageBuckets;
    protected final int metabolismRateBP; 
    protected final int reproductionRateBP; 

    protected SwarmOrganism(Configuration config, ComponentRegistry registry, String typeName, SpeciesKey speciesKey, long maxBiomass, 
                            int speed, int maxAge, int metabolismRateBP, int reproductionRateBP) {
        super(config, registry, typeName, speciesKey, maxBiomass, speed);
        this.ageBuckets = new long[maxAge + 1];
        this.metabolismRateBP = metabolismRateBP;
        this.reproductionRateBP = reproductionRateBP;
    }

    public void tick(Cell cell) {
        processLifecycle(cell);
    }

    protected void processLifecycle(Cell cell) {
        final long oldBiomass = getBiomass();
        applyMetabolism();
        processFeeding(cell);
        advanceAge(cell);
        processReproduction(cell);
        updateTotalBiomass();
        reportChange(cell, getBiomass() - oldBiomass);
    }

    protected void applyMetabolism() {
        for (int i = 0; i < ageBuckets.length; i++) {
            ageBuckets[i] = (ageBuckets[i] * (config.getScale10K() - metabolismRateBP)) / config.getScale10K();
        }
    }

    protected abstract void processFeeding(Cell cell);

    protected void advanceAge(Cell cell) {
        for (int i = ageBuckets.length - 1; i > 0; i--) {
            ageBuckets[i] = ageBuckets[i - 1];
        }
        ageBuckets[0] = 0; 
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
    public void addBiomass(long amount, Cell cell) {
        if (amount > 0) {
            long newTotal = getBiomass() + amount;
            if (maxBiomass > 0 && newTotal > maxBiomass) {
                amount = maxBiomass - getBiomass();
            }
            if (amount > 0) {
                ageBuckets[0] += amount;
                updateTotalBiomass();
                reportChange(cell, amount);
            }
        }
    }

    @Override
    public long consumeBiomass(long amount, Cell cell) {
        long total = getBiomass();
        long actualEaten = Math.min(total, amount);
        if (actualEaten > 0 && total > 0) {
            long remaining = total - actualEaten;
            for (int i = 0; i < ageBuckets.length; i++) {
                ageBuckets[i] = (ageBuckets[i] * remaining) / total;
            }
            updateTotalBiomass();
            reportChange(cell, -actualEaten);
        }
        return actualEaten;
    }

    private void reportChange(Cell cell, long delta) {
        if (delta != 0) {
            NatureWorld nw = (NatureWorld) cell.getWorld();
            nw.getStatisticsService().registerBiomassChange(getSpeciesKey(), delta);
        }
    }
}