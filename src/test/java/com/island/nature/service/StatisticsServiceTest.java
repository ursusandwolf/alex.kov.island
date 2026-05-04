package com.island.nature.service;

import com.island.nature.config.Configuration;
import com.island.nature.entities.SpeciesKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsServiceTest {
    private StatisticsService statisticsService;
    private Configuration config;

    @BeforeEach
    void setUp() {
        config = new Configuration();
        statisticsService = new StatisticsService(config);
    }

    @Test
    void shouldCorrectlyMergeAnimalAndBiomassCounts() {
        SpeciesKey key = SpeciesKey.WOLF;
        
        // 5 alive wolves
        for (int i = 0; i < 5; i++) {
            statisticsService.registerBirth(key);
        }
        
        // Biomass equivalent to 2 wolves
        statisticsService.registerBiomassChange(key, 2 * config.getScale1M());
        
        Map<SpeciesKey, Integer> counts = statisticsService.getSpeciesCountsMap();
        assertEquals(7, counts.get(key), "Count should be sum of animals and biomass units");
        assertEquals(7, statisticsService.getTotalPopulation(), "Total population should match merged count");
    }

    @Test
    void shouldHandleNegativeBiomassCorrectly() {
        SpeciesKey key = SpeciesKey.PLANT;
        statisticsService.registerBiomassChange(key, 5 * config.getScale1M());
        statisticsService.registerBiomassChange(key, -2 * config.getScale1M());
        
        assertEquals(3, statisticsService.getSpeciesCount(key));
        assertEquals(3, statisticsService.getTotalPopulation());
    }
}
