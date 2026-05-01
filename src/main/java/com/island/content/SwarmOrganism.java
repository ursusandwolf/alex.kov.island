package com.island.content;

import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.engine.SimulationNode;
import lombok.Getter;

@Getter
public abstract class SwarmOrganism extends Biomass {
    protected final long[] ageBuckets;
    protected final int metabolismRateBP; 
    protected final int reproductionRateBP; 

    protected SwarmOrganism(String typeName, SpeciesKey speciesKey, long maxBiomass, 
                            int speed, int maxAge, int metabolismRateBP, int reproductionRateBP) {
        super(typeName, speciesKey, maxBiomass, speed);
        this.ageBuckets = new long[maxAge + 1];
        this.metabolismRateBP = metabolismRateBP;
        this.reproductionRateBP = reproductionRateBP;
    }

    @Override
    public void tick(SimulationNode<Organism> node) {
        processLifecycle(node);
    }

    protected void processLifecycle(SimulationNode<Organism> node) {
        final long oldBiomass = getBiomass();
        applyMetabolism();
        processFeeding(node);
        advanceAge(node);
        processReproduction(node);
        updateTotalBiomass();
        reportChange(node, getBiomass() - oldBiomass);
    }

    protected void applyMetabolism() {
        for (int i = 0; i < ageBuckets.length; i++) {
            ageBuckets[i] = (ageBuckets[i] * (SCALE_10K - metabolismRateBP)) / SCALE_10K;
        }
    }

    protected abstract void processFeeding(SimulationNode<Organism> node);

    protected void advanceAge(SimulationNode<Organism> node) {
        for (int i = ageBuckets.length - 1; i > 0; i--) {
            ageBuckets[i] = ageBuckets[i - 1];
        }
        ageBuckets[0] = 0; 
    }

    protected abstract void processReproduction(SimulationNode<Organism> node);

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
    public void addBiomass(long amount, SimulationNode<Organism> node) {
        if (amount > 0) {
            long newTotal = getBiomass() + amount;
            if (maxBiomass > 0 && newTotal > maxBiomass) {
                amount = maxBiomass - getBiomass();
            }
            if (amount > 0) {
                ageBuckets[0] += amount;
                updateTotalBiomass();
                reportChange(node, amount);
            }
        }
    }

    @Override
    public long consumeBiomass(long amount, SimulationNode<Organism> node) {
        long total = getBiomass();
        long actualEaten = Math.min(total, amount);
        if (actualEaten > 0 && total > 0) {
            long remaining = total - actualEaten;
            for (int i = 0; i < ageBuckets.length; i++) {
                ageBuckets[i] = (ageBuckets[i] * remaining) / total;
            }
            updateTotalBiomass();
            reportChange(node, -actualEaten);
        }
        return actualEaten;
    }

    private void reportChange(SimulationNode<Organism> node, long delta) {
        if (delta != 0 && node.getWorld() instanceof NatureWorld nw) {
            nw.getStatisticsService().registerBiomassChange(getSpeciesKey(), delta);
        }
    }
}
