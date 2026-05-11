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

        // Add one residential building and ALL infrastructure at (0,0) to ensure power/water
        SimEntity building = new SimEntity(map.getComponentRegistry());
        building.addComponent(BuildingComponent.builder().type(BuildingComponent.Type.RESIDENTIAL).build());
        
        SimEntity road00 = new SimEntity(map.getComponentRegistry());
        road00.addComponent(BuildingComponent.builder().type(BuildingComponent.Type.ROAD).build());

        SimEntity powerPlant = new SimEntity(map.getComponentRegistry());
        powerPlant.addComponent(BuildingComponent.builder().type(BuildingComponent.Type.POWER_PLANT).build());

        SimEntity waterPipe = new SimEntity(map.getComponentRegistry());
        waterPipe.addComponent(BuildingComponent.builder().type(BuildingComponent.Type.WATER_PIPE).build());
        
        map.getGrid()[0][0].addEntity(building);
        map.getGrid()[0][0].addEntity(road00);
        map.getGrid()[0][0].addEntity(powerPlant);
        map.getGrid()[0][0].addEntity(waterPipe);

        // Add industrial buildings far away to provide jobs without polluting the residential area
        SimEntity factory1 = new SimEntity(map.getComponentRegistry());
        factory1.addComponent(BuildingComponent.builder().type(BuildingComponent.Type.INDUSTRIAL).build());
        map.getGrid()[4][4].addEntity(factory1);
        
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

        // In 10 ticks, population should have reached 5 (cap for LOW density)
        assertEquals(5, population, "Population should reach the cap of 5 in the residential cell. Desirability: " + map.getGrid()[0][0].getDesirability());
        
        System.out.println("Smoke test passed: Population=" + population + ", Money=" + map.getMoney());
        context.gameLoop().stop();
    }
}
