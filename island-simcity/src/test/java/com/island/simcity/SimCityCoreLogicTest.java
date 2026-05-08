package com.island.simcity;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.service.BuildingService;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.engine.parallel.ParallelDispatcher;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.PhaseScheduler;

class SimCityCoreLogicTest {

    @Test
    @DisplayName("Should deduct money when building via BuildingService")
    void should_deduct_money_on_build() {
        // Given
        ComponentRegistry registry = new ComponentRegistry();
        CityMap map = new CityMap(5, 5, new DefaultEventBus(), registry);
        BuildingService buildingService = new BuildingService(map);
        long initialMoney = map.getMoney();

        // When
        boolean success = buildingService.build(0, 0, BuildingComponent.Type.RESIDENTIAL);

        // Then
        assertTrue(success);
        assertEquals(initialMoney - 200, map.getMoney(), "Residential building should cost 200");
    }

    @Test
    @DisplayName("Should trigger bankruptcy when money is negative for several ticks")
    void should_trigger_bankruptcy_logic() {
        // Given
        CityMap map = new CityMap(5, 5, new DefaultEventBus(), new ComponentRegistry());
        map.addMoney(-20000); // Force negative balance

        // When & Then
        for (int i = 0; i < 4; i++) {
            map.tick(i);
            assertFalse(map.isBankrupt(), "Should not be bankrupt before threshold");
        }

        map.tick(5);
        assertTrue(map.isBankrupt(), "Should be bankrupt after 5 ticks of negative balance");
        assertTrue(map.getAlerts().contains("CITY BANKRUPT!"));
    }

    @Test
    @DisplayName("Residents should leave when happiness is low due to high taxes")
    void residents_should_leave_on_high_taxes() {
        // Given
        ComponentRegistry registry = new ComponentRegistry();
        CityMap map = new CityMap(5, 5, new DefaultEventBus(), registry);
        map.setTaxRate(50); // Very high tax
        map.getGrid()[0][0].setConnected(true);
        
        SimEntity resident = new SimEntity(registry);
        PopulationComponent pop = new PopulationComponent(0, 20);
        resident.addComponent(pop);
        map.getGrid()[0][0].addEntity(resident);

        PopulationService popService = new PopulationService(map, registry);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ParallelDispatcher<SimEntity> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<SimEntity> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<SimEntity> loop = new GameLoop<>(0, executor, scheduler);
        loop.setWorld(map);
        loop.addRecurringTask(popService);

        // When
        loop.runTick(); // Tick 1 (resident update age, happiness)
        loop.runTick(); // Tick 2 (migration out check)

        // Then
        assertFalse(resident.isAlive(), "Resident should 'die' (leave) due to low happiness and high taxes");
    }

    @Test
    @DisplayName("Concurrency test: multiple threads updating money should be safe")
    void money_updates_should_be_thread_safe() throws InterruptedException {
        // Given
        CityMap map = new CityMap(5, 5, new DefaultEventBus(), new ComponentRegistry());
        int threadCount = 10;
        int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    map.addMoney(1);
                }
                latch.countDown();
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertEquals(10000 + (threadCount * incrementsPerThread), map.getMoney(), 
                "Money should be exactly 10000 + 10000 = 20000");
    }
}