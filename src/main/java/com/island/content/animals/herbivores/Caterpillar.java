package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.model.Cell;
import java.util.List;

/**
 * Optimized Caterpillar: Now acts as a biomass container with stages.
 * Life cycle: 40 ticks active -> 20 ticks sleep (50% probability) -> Butterfly.
 */
public class Caterpillar extends Biomass {
    private final double[] activeStages = new double[40];
    private final double[] sleepStages = new double[20];

    public Caterpillar(double maxBiomass, int speed) {
        super("Caterpillar", SpeciesKey.CATERPILLAR, maxBiomass, speed);
        // Distribute initial biomass across active stages for realistic start
        activeStages[0] = maxBiomass;
        this.biomass = maxBiomass;
    }

    public void processPendulum(Cell cell) {
        // 1. Natural metabolic loss (only for active stages)
        final double oldBiomass = biomass;
        double totalActive = 0;
        for (int i = 0; i < activeStages.length; i++) {
            activeStages[i] *= (1.0 - CATERPILLAR_METABOLISM_RATE);
            totalActive += activeStages[i];
        }

        // 2. Feed on actual plants (increases biomass in the first active stage)
        double appetite = totalActive * 0.10; 
        if (appetite > 0) {
            List<Biomass> availablePlants = cell.getBiomassContainers();
            for (Biomass p : availablePlants) {
                if (p != this && p.isAlive() && !(p instanceof Butterfly)) {
                    double eaten = p.consumeBiomass(appetite * (1.0 / CATERPILLAR_FEED_EFFICIENCY), cell);
                    activeStages[0] += (eaten * CATERPILLAR_FEED_EFFICIENCY);
                    appetite -= eaten;
                    if (appetite <= 0) {
                        break;
                    }
                }
            }
        }

        // 3. Life Cycle Advancement
        advanceStages(cell);
        
        // 4. Sync total biomass
        updateTotalBiomass();
        reportChange(cell, biomass - oldBiomass);
    }

    private void advanceStages(Cell cell) {
        // --- Sleep to Butterfly transition ---
        double emergingButterflies = sleepStages[sleepStages.length - 1];
        if (emergingButterflies > 0) {
            Butterfly b = (Butterfly) cell.getBiomass(SpeciesKey.BUTTERFLY);
            if (b == null) {
                b = new Butterfly(0, this.speed);
                cell.addBiomass(b);
            }
            b.addBiomass(emergingButterflies, cell);
        }

        // Shift sleep stages
        for (int i = sleepStages.length - 1; i > 0; i--) {
            sleepStages[i] = sleepStages[i - 1];
        }
        sleepStages[0] = 0;

        // --- Active to Sleep transition ---
        double maturingCaterpillars = activeStages[activeStages.length - 1];
        // 50% fall into hibernation, 50% stay active (reset to age 0)
        sleepStages[0] = maturingCaterpillars * 0.5;
        double stayedActive = maturingCaterpillars * 0.5;

        // Shift active stages
        for (int i = activeStages.length - 1; i > 0; i--) {
            activeStages[i] = activeStages[i - 1];
        }
        activeStages[0] = stayedActive;
    }

    private void updateTotalBiomass() {
        double total = 0;
        for (double s : activeStages) {
            total += s;
        }
        for (double s : sleepStages) {
            total += s;
        }
        this.biomass = total;
    }

    @Override
    public void tick(Cell cell) {
        processPendulum(cell);
    }

    @Override
    public boolean isHibernating() {
        // We consider the mass in sleepStages as hibernating
        double sleepTotal = 0;
        for (double s : sleepStages) {
            sleepTotal += s;
        }
        return sleepTotal > (biomass * 0.1); // Simple threshold
    }

    public void spawn(double amount, Cell cell) {
        if (amount > 0) {
            activeStages[0] += amount;
            double old = biomass;
            updateTotalBiomass();
            reportChange(cell, biomass - old);
        }
    }

    @Override
    public double consumeBiomass(double amount, Cell cell) {
        double activeTotal = 0;
        for (double s : activeStages) {
            activeTotal += s;
        }

        double actualEaten = Math.min(activeTotal, amount);
        if (actualEaten > 0) {
            double factor = (activeTotal - actualEaten) / activeTotal;
            for (int i = 0; i < activeStages.length; i++) {
                activeStages[i] *= factor;
            }
            double old = biomass;
            updateTotalBiomass();
            reportChange(cell, biomass - old);
        }
        return actualEaten;
    }

    private void reportChange(Cell cell, double delta) {
        if (delta != 0) {
            cell.getWorld().getStatisticsService().registerBiomassChange(speciesKey, delta);
        }
    }
}
