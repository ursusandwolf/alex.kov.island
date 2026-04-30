package com.island.service;

import com.island.content.DeathCause;
import com.island.content.SpeciesKey;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import lombok.Getter;

/**
 * Service responsible for gathering and providing simulation statistics.
 * Decouples reporting logic from the world model.
 */
public class StatisticsService {
    @Getter
    private final Map<SpeciesKey, AtomicInteger> speciesCounts = new ConcurrentHashMap<>();
    private final Map<SpeciesKey, DoubleAdder> biomassMass = new ConcurrentHashMap<>();
    
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> tickDeathStats = new EnumMap<>(DeathCause.class);
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> totalDeathStats = new EnumMap<>(DeathCause.class);

    public StatisticsService() {
        for (DeathCause cause : DeathCause.values()) {
            tickDeathStats.put(cause, new ConcurrentHashMap<>());
            totalDeathStats.put(cause, new ConcurrentHashMap<>());
        }
    }

    public void registerBirth(SpeciesKey speciesKey) {
        speciesCounts.computeIfAbsent(speciesKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Decrements the species count without recording it as a death from a specific cause.
     * Used during movement or when an organism is recycled/removed from the world.
     */
    public void registerRemoval(SpeciesKey speciesKey) {
        AtomicInteger count = speciesCounts.get(speciesKey);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    public void registerBiomassChange(SpeciesKey speciesKey, double delta) {
        biomassMass.computeIfAbsent(speciesKey, k -> new DoubleAdder()).add(delta);
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
        // Clear only per-tick stats, keep totals and counts
        for (Map<SpeciesKey, AtomicInteger> stats : tickDeathStats.values()) {
            stats.clear();
        }
    }

    public int getSpeciesCount(SpeciesKey key) {
        AtomicInteger count = speciesCounts.get(key);
        int animalCount = (count != null) ? Math.max(0, count.get()) : 0;
        
        DoubleAdder biomass = biomassMass.get(key);
        int mass = (biomass != null) ? (int) Math.max(0, biomass.sum()) : 0;
        
        return animalCount + mass;
    }

    public int getTotalPopulation() {
        int animals = speciesCounts.values().stream().mapToInt(AtomicInteger::get).map(c -> Math.max(0, c)).sum();
        int biomass = (int) biomassMass.values().stream().mapToDouble(DoubleAdder::sum).map(d -> Math.max(0, d)).sum();
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
            int mass = (int) v.sum();
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
