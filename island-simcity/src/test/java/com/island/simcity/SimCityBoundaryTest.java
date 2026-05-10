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
        
        // Add infrastructure
        SimEntity road = new SimEntity(map.getComponentRegistry());
        road.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[0][0].addEntity(road);

        SimEntity pipe = new SimEntity(map.getComponentRegistry());
        pipe.addComponent(new BuildingComponent(BuildingComponent.Type.WATER_PIPE));
        map.getGrid()[0][0].addEntity(pipe);

        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);
        
        SimEntity resident = new SimEntity(map.getComponentRegistry());
        PopulationComponent pop = new PopulationComponent(0, 100);
        resident.addComponent(pop);
        map.getGrid()[0][0].addEntity(resident);
        
        // Run several ticks
        for (int i = 0; i < 5; i++) {
            context.gameLoop().runTick();
        }
        
        assertTrue(pop.getHappiness() < 100, "Happiness should drop significantly at 100% tax, was: " + pop.getHappiness());
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Communication: Residents should be unhappy without water")
    void residents_unhappy_without_water() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // Add road and power but no water
        SimEntity road = new SimEntity(map.getComponentRegistry());
        road.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[0][0].addEntity(road);

        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);
        
        SimEntity resident = new SimEntity(map.getComponentRegistry());
        PopulationComponent pop = new PopulationComponent(0, 100);
        resident.addComponent(pop);
        map.getGrid()[0][0].addEntity(resident);
        
        context.gameLoop().runTick();
        
        // Initial 100, no water (-20), connected (+2) = 82
        assertTrue(pop.getHappiness() <= 82, "Happiness should drop without water, was: " + pop.getHappiness());
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Communication: Metro should provide significant happiness bonus")
    void metro_happiness_bonus() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // Add road, water, power and metro
        SimEntity road = new SimEntity(map.getComponentRegistry());
        road.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[0][0].addEntity(road);

        SimEntity pipe = new SimEntity(map.getComponentRegistry());
        pipe.addComponent(new BuildingComponent(BuildingComponent.Type.WATER_PIPE));
        map.getGrid()[0][0].addEntity(pipe);

        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);

        SimEntity metro = new SimEntity(map.getComponentRegistry());
        metro.addComponent(new BuildingComponent(BuildingComponent.Type.METRO));
        map.getGrid()[0][0].addEntity(metro);

        SimEntity resident = new SimEntity(map.getComponentRegistry());
        PopulationComponent pop = new PopulationComponent(0, 70);
        resident.addComponent(pop);
        map.getGrid()[0][0].addEntity(resident);
        
        context.gameLoop().runTick();
        context.gameLoop().runTick();
        context.gameLoop().runTick();
        context.gameLoop().runTick();
        context.gameLoop().runTick();
        context.gameLoop().runTick();
        
        assertEquals(100, pop.getHappiness(), "Happiness should be capped at 100");
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Electricity: Power should spread through continuous buildings")
    void power_spread_through_buildings() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // (0,0) Power Plant
        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);

        // (1,0) Residential
        SimEntity res1 = new SimEntity(map.getComponentRegistry());
        res1.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        map.getGrid()[1][0].addEntity(res1);

        // (2,0) Residential
        SimEntity res2 = new SimEntity(map.getComponentRegistry());
        res2.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        map.getGrid()[2][0].addEntity(res2);

        context.gameLoop().runTick();

        assertTrue(map.getGrid()[0][0].isPowered());
        assertTrue(map.getGrid()[1][0].isPowered());
        assertTrue(map.getGrid()[2][0].isPowered());
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Electricity: Power should NOT spread through empty space")
    void power_not_spread_through_empty() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // (0,0) Power Plant
        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);

        // (1,0) EMPTY

        // (2,0) Residential
        SimEntity res = new SimEntity(map.getComponentRegistry());
        res.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        map.getGrid()[2][0].addEntity(res);

        context.gameLoop().runTick();

        assertTrue(map.getGrid()[0][0].isPowered());
        assertFalse(map.getGrid()[2][0].isPowered(), "Power should not jump over empty tile");
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Electricity: Power should NOT spread through roads")
    void power_not_spread_through_roads() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // (0,0) Power Plant
        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);

        // (1,0) Road
        SimEntity road = new SimEntity(map.getComponentRegistry());
        road.addComponent(new BuildingComponent(BuildingComponent.Type.ROAD));
        map.getGrid()[1][0].addEntity(road);

        // (2,0) Residential
        SimEntity res = new SimEntity(map.getComponentRegistry());
        res.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        map.getGrid()[2][0].addEntity(res);

        context.gameLoop().runTick();

        assertTrue(map.getGrid()[0][0].isPowered());
        assertFalse(map.getGrid()[1][0].isPowered(), "Roads are not conductive");
        assertFalse(map.getGrid()[2][0].isPowered(), "Power should not spread through roads");
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Electricity: Power should spread through Power Lines")
    void power_spread_through_lines() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // (0,0) Power Plant
        SimEntity plant = new SimEntity(map.getComponentRegistry());
        plant.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_PLANT));
        map.getGrid()[0][0].addEntity(plant);

        // (1,0) Power Line
        SimEntity line = new SimEntity(map.getComponentRegistry());
        line.addComponent(new BuildingComponent(BuildingComponent.Type.POWER_LINE));
        map.getGrid()[1][0].addEntity(line);

        // (2,0) Residential
        SimEntity res = new SimEntity(map.getComponentRegistry());
        res.addComponent(new BuildingComponent(BuildingComponent.Type.RESIDENTIAL));
        map.getGrid()[2][0].addEntity(res);

        context.gameLoop().runTick();

        assertTrue(map.getGrid()[0][0].isPowered());
        assertTrue(map.getGrid()[1][0].isPowered());
        assertTrue(map.getGrid()[2][0].isPowered());
        context.gameLoop().stop();
    }

    @Test
    @DisplayName("Pollution: Industrial zones should drop nearby happiness")
    void pollution_drops_happiness() {
        SimulationEngine<SimEntity> engine = new SimulationEngine<>();
        SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(5, 5), 0, 1);
        CityMap map = (CityMap) context.world();
        
        // Setup infrastructure for resident (no bonuses to make them sensitive to pollution)
        map.getGrid()[0][0].setConnected(true);
        
        SimEntity resident = new SimEntity(map.getComponentRegistry());
        PopulationComponent pop = new PopulationComponent(0, 100);
        resident.addComponent(pop);
        map.getGrid()[0][0].addEntity(resident);

        // Place polluting industry at (0,1)
        SimEntity industry = new SimEntity(map.getComponentRegistry());
        industry.addComponent(new BuildingComponent(BuildingComponent.Type.INDUSTRIAL));
        map.getGrid()[0][1].addEntity(industry);

        // Run many ticks to accumulate pollution (50 ticks)
        for (int i = 0; i < 50; i++) {
            context.gameLoop().runTick();
        }

        assertTrue(map.getGrid()[0][1].getAirPollution() > 0, "Industry should generate air pollution");
        assertTrue(map.getGrid()[0][0].getAirPollution() > 0, "Pollution should spread to neighbor");
        assertTrue(pop.getHappiness() < 100, "Resident happiness should drop due to pollution, was: " + pop.getHappiness());
        
        context.gameLoop().stop();
    }
}
