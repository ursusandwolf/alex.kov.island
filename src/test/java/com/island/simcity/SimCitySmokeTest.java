package com.island.simcity;

import com.island.engine.GameLoop;
import com.island.simcity.entities.Building;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimCitySmokeTest {

    @Test
    void testCityGrowthAndEconomy() {
        // 1. Setup
        CityMap map = new CityMap(5, 5);
        map.initialize();
        map.setResDemand(50); // Ensure demand for growth
        
        PopulationService popService = new PopulationService(map);
        EconomyService economyService = new EconomyService(map);

        GameLoop<SimEntity> gameLoop = new GameLoop<>(0, 1);
        gameLoop.setWorld(map);
        gameLoop.addRecurringTask(popService);
        gameLoop.addRecurringTask(economyService);

        // Add one residential building and mark as connected for simplicity
        map.getGrid()[0][0].addEntity(new Building(Building.Type.RESIDENTIAL));
        map.getGrid()[0][0].setConnected(true);
        
        long initialMoney = map.getMoney();

        // 2. Run simulation for 10 ticks
        for (int i = 0; i < 10; i++) {
            gameLoop.runTick();
        }

        // 3. Verify
        int population = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                population += (int) map.getGrid()[x][y].getEntities().stream()
                        .filter(e -> e.getTypeName().equals("Resident"))
                        .count();
            }
        }

        // In 10 ticks, population should have reached 5 (it spawns 1 per tick until 5)
        assertEquals(5, population, "Population should reach the cap of 5 in the residential cell");
        assertTrue(map.getMoney() > initialMoney, "Money should increase due to taxes");
        
        System.out.println("Smoke test passed: Population=" + population + ", Money=" + map.getMoney());
    }
}
