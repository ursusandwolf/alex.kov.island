package com.island.simcity;

import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimCitySmokeTest {

    @Test
    void testCityGrowthAndEconomy() {
        // 1. Setup using the public SimulationEngine API
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        
        CityMap map = (CityMap) context.world();
        map.setResDemand(50); // Ensure demand for growth
        
        long initialMoney = map.getMoney();

        // Add one residential building and basic infrastructure at (0,0)
        // ConnectivityService starts all networks from (0,0)
        SimEntity building = new SimEntity(map.getComponentRegistry());
        building.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        
        SimEntity road00 = new SimEntity(map.getComponentRegistry());
        road00.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));

        SimEntity powerPlant = new SimEntity(map.getComponentRegistry());
        powerPlant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));

        SimEntity waterPipe = new SimEntity(map.getComponentRegistry());
        waterPipe.addComponent(new BuildingComponent(BuildingComponent.Type.WATER_PIPE));
        
        map.getGrid()[0][0].addEntity(building);
        map.getGrid()[0][0].addEntity(road00);
        map.getGrid()[0][0].addEntity(powerPlant);
        map.getGrid()[0][0].addEntity(waterPipe);

        // Add an industrial building to create demand for residents
        SimEntity factory = new SimEntity(map.getComponentRegistry());
        factory.addComponent(new BuildingComponent(BuildingComponent.Type.INDUSTRIAL));
        map.getGrid()[1][1].addEntity(factory);
        SimEntity road11 = new SimEntity(map.getComponentRegistry());
        road11.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[1][1].addEntity(road11);
        
        // Connectivity between (0,0) and (1,1) via (0,1) or (1,0)
        SimEntity road01 = new SimEntity(map.getComponentRegistry());
        road01.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[0][1].addEntity(road01);
        
        // 2. Run simulation for 20 ticks
        for (int i = 0; i < 20; i++) {
            context.gameLoop().runTick();
        }

        // 3. Verify
        int population = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                population += (int) map.getGrid()[x][y].getEntities().stream()
                        .filter(e -> e.hasComponent(PopulationComponent.class))
                        .count();
            }
        }

        // In 10 ticks, population should have reached 5 (it spawns 1 per tick until 5)
        assertEquals(5, population, "Population should reach the cap of 5 in the residential cell");
        assertTrue(map.getMoney() > initialMoney, "Money should increase due to taxes");
        
        System.out.println("Smoke test passed: Population=" + population + ", Money=" + map.getMoney());
        context.gameLoop().stop();
    }
}
