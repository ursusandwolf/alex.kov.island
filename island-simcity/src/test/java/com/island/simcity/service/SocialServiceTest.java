package com.island.simcity.service;

import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import com.island.engine.event.EventBus;
import com.island.engine.ecs.ComponentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialServiceTest {
    private CityMap map;
    private SocialService socialService;
    private SchoolEffectProvider schoolProvider;

    @BeforeEach
    void setUp() {
        ComponentRegistry registry = new ComponentRegistry();
        map = new CityMap(5, 5, EventBus.create(), registry);
        schoolProvider = new SchoolEffectProvider();
        socialService = new SocialService(map, List.of(schoolProvider));
    }

    @Test
    void shouldApplySchoolEffect() {
        CityTile tile = map.getGrid()[2][2];
        SimEntity school = new SimEntity(new ComponentRegistry());
        school.addComponent(BuildingComponent.builder().type(BuildingComponent.Type.SCHOOL).build());
        tile.addEntity(school);

        socialService.beforeTick(1);
        socialService.processCell(tile, 1);

        // Center tile effect (60)
        assertEquals(60, tile.getEducationLevel());
        
        // Neighbor tile effect (spread radius 1, power 30, attenuated by distance)
        // Distance 1: 30/1 = 30
        assertEquals(30, map.getGrid()[2][3].getEducationLevel());
        assertEquals(30, map.getGrid()[3][2].getEducationLevel());
    }

    @Test
    void shouldResetLevelsBeforeTick() {
        CityTile tile = map.getGrid()[0][0];
        tile.setEducationLevel(100);
        
        socialService.beforeTick(1);
        
        assertEquals(0, tile.getEducationLevel());
    }
}
