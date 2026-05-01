package com.island.nature.service;

import static com.island.nature.config.SimulationConstants.SCALE_1M;

import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SimulationMetrics;
import com.island.nature.entities.SpeciesKey;
import com.island.engine.SimulationWorld;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;

/**
 * Service responsible for gathering and providing simulation statistics using integer arithmetic.
 * Biomass is tracked as long (SCALE_1M).
 */
public class StatisticsService {
    @Getter
    private final Map<SpeciesKey, AtomicInteger> speciesCounts = new ConcurrentHashMap<>();
    private final Map<SpeciesKey, LongAdder> biomassMass = new ConcurrentHashMap<>();
    
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> tickDeathStats = new EnumMap<>(DeathCause.class);
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> totalDeathStats = new EnumMap<>(DeathCause.class);

    @Getter
    private volatile SimulationMetrics latestMetrics = SimulationMetrics.empty();

    public StatisticsService() {
        for (DeathCause cause : DeathCause.values()) {
            tickDeathStats.put(cause, new ConcurrentHashMap<>());
            totalDeathStats.put(cause, new ConcurrentHashMap<>());
        }
    }

    public void updateMetrics(SimulationMetrics metrics) {
        this.latestMetrics = metrics;
    }

    public void registerBirth(SpeciesKey speciesKey) {
        speciesCounts.computeIfAbsent(speciesKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void registerRemoval(SpeciesKey speciesKey) {
        AtomicInteger count = speciesCounts.get(speciesKey);
        if (count != null) {
            count.updateAndGet(v -> Math.max(0, v - 1));
        }
    }

    public void registerBiomassChange(SpeciesKey speciesKey, long delta) {
        biomassMass.computeIfAbsent(speciesKey, k -> new LongAdder()).add(delta);
    }

    public void registerDeath(SpeciesKey speciesKey, DeathCause cause) {
        registerRemoval(speciesKey);
        
        tickDeathStats.get(cause)
                 .computeIfAbsent(speciesKey, k -> new AtomicInteger(0))
                 .incrementAndGet();
        totalDeathStats.get(cause)
                 .computeIfAbsent(speciesKey, k -> new AtomicInteger(0))
                 .incrementAndGet();
    }

    public void onTickStarted() {
        for (Map<SpeciesKey, AtomicInteger> stats : tickDeathStats.values()) {
            stats.clear();
        }
    }

    public int getSpeciesCount(SpeciesKey key) {
        AtomicInteger count = speciesCounts.get(key);
        int animalCount = (count != null) ? Math.max(0, count.get()) : 0;
        
        LongAdder biomass = biomassMass.get(key);
        int mass = (biomass != null) ? (int) Math.max(0, biomass.sum() / SCALE_1M) : 0;
        
        return animalCount + mass;
    }

    public double calculateGlobalSatiety(SimulationWorld<Organism> world) {
        return latestMetrics.getGlobalSatiety();
    }

    public int calculateStarvingCount(SimulationWorld<Organism> world) {
        return latestMetrics.getStarvingCount();
    }

    public int getTotalPopulation() {
        int animals = speciesCounts.values().stream().mapToInt(AtomicInteger::get).map(c -> Math.max(0, c)).sum();
        int biomass = (int) (biomassMass.values().stream().mapToLong(LongAdder::sum).map(l -> Math.max(0, l)).sum() / SCALE_1M);
        return animals + biomass;
    }

    public Map<SpeciesKey, Integer> getSpeciesCountsMap() {
        Map<SpeciesKey, Integer> counts = new HashMap<>();
        speciesCounts.forEach((k, v) -> {
            if (v.get() > 0) {
                counts.put(k, v.get());
            }
        });
        biomassMass.forEach((k, v) -> {
            int mass = (int) (v.sum() / SCALE_1M);
            if (mass > 0) {
                counts.merge(k, mass, Integer::sum);
            }
        });
        return counts;
    }

    public Map<SpeciesKey, Integer> getTickDeaths(DeathCause cause) {
        Map<SpeciesKey, Integer> result = new HashMap<>();
        tickDeathStats.get(cause).forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public Map<SpeciesKey, Integer> getTotalDeaths(DeathCause cause) {
        Map<SpeciesKey, Integer> result = new HashMap<>();
        totalDeathStats.get(cause).forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
}
