package com.island.simcity;

import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.service.BuildingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimCityBoundaryTest {

    @Test
    @DisplayName("Economy Boundary: Should build when money is exactly the cost")
    void should_build_when_money_is_exactly_cost() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        BuildingService buildingService = new BuildingService(map);
        
        map.setMoney(100); // Agricultural cost is 100
        boolean success = buildingService.build(0, 0, BuildingComponent.Type.AGRICULTURAL);
        
        assertTrue(success, "Should succeed when money == cost");
        assertEquals(0, map.getMoney(), "Money should be exactly 0 after build");
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Economy Boundary: Should not build when money is cost - 1")
    void should_not_build_when_money_is_insufficient() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        BuildingService buildingService = new BuildingService(map);
        
        map.setMoney(199); // Residential cost is 200
        boolean success = buildingService.build(0, 0, BuildingComponent.Type.RESIDENTIAL);
        
        assertFalse(success, "Should fail when money < cost");
        assertEquals(199, map.getMoney(), "Money should not be deducted");
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Spatial Boundary: Building at (0,0) and (width-1, height-1)")
    void spatial_boundary_corners() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        int w = 5, h = 5;
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(w, h), 0, 1);
        CityMap map = (CityMap) context.world();
        BuildingService buildingService = new BuildingService(map);
        
        assertTrue(buildingService.build(0, 0, BuildingComponent.Type.ROAD));
        assertTrue(buildingService.build(w - 1, h - 1, BuildingComponent.Type.ROAD));
        
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Spatial Boundary: Building outside grid should fail")
    void spatial_boundary_outside() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        int w = 5, h = 5;
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(w, h), 0, 1);
        CityMap map = (CityMap) context.world();
        BuildingService buildingService = new BuildingService(map);
        
        assertFalse(buildingService.build(-1, 0, BuildingComponent.Type.ROAD));
        assertFalse(buildingService.build(0, -1, BuildingComponent.Type.ROAD));
        assertFalse(buildingService.build(w, 0, BuildingComponent.Type.ROAD));
        assertFalse(buildingService.build(0, h, BuildingComponent.Type.ROAD));
        
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Density Boundary: Cannot build two buildings on same tile")
    void density_boundary_collision() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        BuildingService buildingService = new BuildingService(map);
        
        assertTrue(buildingService.build(2, 2, BuildingComponent.Type.RESIDENTIAL));
        assertFalse(buildingService.build(2, 2, BuildingComponent.Type.COMMERCIAL), "Should not build on top of another");
        
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Social Boundary: Tax at 0% should result in no income from residents")
    void tax_at_zero() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        map.setTaxRate(0);
        map.setMoney(0);
        
        // Add a resident
        map.getGrid()[0][0].setConnected(true);
        SimEntity resident = new SimEntity(map.getComponentRegistry());
        resident.addComponent(new PopulationComponent(0, 100));
        map.getGrid()[0][0].addEntity(resident);
        
        context.gameLoop().runTick();
        
        // Residents pay tax = taxRate. If taxRate is 0, income from residents is 0.
        // There might be expenses for the tile if there are buildings, but here we only have a resident.
        assertEquals(0, map.getMoney(), "No income from residents when tax is 0");
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Social Boundary: Tax at 100% should crash happiness")
    void tax_at_hundred() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        map.setTaxRate(100);
        
        map.getGrid()[0][0].setConnected(true);
        SimEntity resident = new SimEntity(map.getComponentRegistry());
        PopulationComponent pop = new PopulationComponent(0, 100);
        resident.addComponent(pop);
        map.getGrid()[0][0].addEntity(resident);
        
        // Run several ticks
        for (int i = 0; i < 5; i++) {
            context.gameLoop().runTick();
        }
        
        assertTrue(pop.getHappiness() < 50, "Happiness should drop significantly at 100% tax");
        context.gameLoop().stop();
    }
}
