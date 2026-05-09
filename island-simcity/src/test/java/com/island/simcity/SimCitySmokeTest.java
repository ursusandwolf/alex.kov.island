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

        // Add one residential building and a road to ensure connectivity
        SimEntity building = new SimEntity(map.getComponentRegistry());
        building.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        
        SimEntity road = new SimEntity(map.getComponentRegistry());
        road.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        
        map.getGrid()[0][0].addEntity(building);
        map.getGrid()[0][0].addEntity(road);

        // Add an industrial building to create demand for residents
        SimEntity factory = new SimEntity(map.getComponentRegistry());
        factory.addComponent(new BuildingComponent(BuildingComponent.Type.INDUSTRIAL));
        map.getGrid()[1][1].addEntity(factory);
        // (1,1) also needs to be connected or ConnectivityService will ignore it
        SimEntity road2 = new SimEntity(map.getComponentRegistry());
        road2.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[0][1].addEntity(road2);
        map.getGrid()[1][1].addEntity(road2); // Wait, same road entity or just same type?
        
        // Let's just put the factory next to the road at (0,1)
        map.getGrid()[0][1].removeEntity(road2); // Cleanup
        SimEntity road01 = new SimEntity(map.getComponentRegistry());
        road01.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[0][1].addEntity(road01);
        
        map.getGrid()[1][1].addEntity(factory);
        // factory at (1,1) is connected because it's adjacent to road at (0,1)
        
        // 2. Run simulation for 10 ticks
        for (int i = 0; i < 10; i++) {
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
