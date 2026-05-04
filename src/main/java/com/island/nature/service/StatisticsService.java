package com.island.nature.service;

import com.island.engine.SimulationWorld;
import com.island.engine.event.EntityBornEvent;
import com.island.engine.event.EntityDiedEvent;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SimulationMetrics;
import com.island.nature.entities.SpeciesKey;
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
    private final Configuration config;
    @Getter
    private final Map<SpeciesKey, AtomicInteger> speciesCounts = new ConcurrentHashMap<>();
    private final Map<SpeciesKey, LongAdder> biomassMass = new ConcurrentHashMap<>();
    
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> tickDeathStats = new EnumMap<>(DeathCause.class);
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> totalDeathStats = new EnumMap<>(DeathCause.class);

    @Getter
    private volatile SimulationMetrics latestMetrics = SimulationMetrics.empty();

    public StatisticsService(Configuration config) {
        this.config = config;
        for (DeathCause cause : DeathCause.values()) {
            tickDeathStats.put(cause, new ConcurrentHashMap<>());
            totalDeathStats.put(cause, new ConcurrentHashMap<>());
        }
    }

    public void subscribe(EventBus bus) {
        bus.subscribe(EntityBornEvent.class, event -> {
            if (event.getEntity() instanceof Animal a) {
                registerBirth(a.getSpeciesKey());
            }
        });
        bus.subscribe(EntityDiedEvent.class, event -> {
            if (event.getEntity() instanceof Animal a) {
                try {
                    DeathCause cause = DeathCause.valueOf(event.getCause());
                    registerDeath(a.getSpeciesKey(), cause);
                } catch (IllegalArgumentException e) {
                    registerRemoval(a.getSpeciesKey());
                }
            }
        });
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
        return getAnimalCount(key) + getBiomassCount(key);
    }

    private int getAnimalCount(SpeciesKey key) {
        AtomicInteger count = speciesCounts.get(key);
        return (count != null) ? Math.max(0, count.get()) : 0;
    }

    private int getBiomassCount(SpeciesKey key) {
        LongAdder biomass = biomassMass.get(key);
        return (biomass != null) ? (int) Math.max(0, biomass.sum() / config.getScale1M()) : 0;
    }

    public double calculateGlobalSatiety(SimulationWorld<Organism, ?> world) {
        return latestMetrics.getGlobalSatiety();
    }

    public int calculateHungryCount(SimulationWorld<Organism, ?> world) {
        return latestMetrics.getHungryCount();
    }

    public int getTotalPopulation() {
        return getSpeciesCountsMap().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public Map<SpeciesKey, Integer> getSpeciesCountsMap() {
        Map<SpeciesKey, Integer> counts = new HashMap<>();
        addAnimalCounts(counts);
        addBiomassCounts(counts);
        return counts;
    }

    private void addAnimalCounts(Map<SpeciesKey, Integer> counts) {
        speciesCounts.forEach((k, v) -> {
            int count = v.get();
            if (count > 0) {
                counts.put(k, count);
            }
        });
    }

    private void addBiomassCounts(Map<SpeciesKey, Integer> counts) {
        biomassMass.forEach((k, v) -> {
            int mass = (int) (v.sum() / config.getScale1M());
            if (mass > 0) {
                counts.merge(k, mass, Integer::sum);
            }
        });
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
