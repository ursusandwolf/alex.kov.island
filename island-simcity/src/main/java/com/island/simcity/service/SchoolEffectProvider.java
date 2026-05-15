package com.island.simcity.service;

import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityTile;
import org.springframework.stereotype.Component;

@Component
public class SchoolEffectProvider implements SocialEffectProvider {
    @Override
    public BuildingComponent.Type getSupportedType() {
        return BuildingComponent.Type.SCHOOL;
    }

    @Override
    public void applyEffect(CityTile tile, SocialService service) {
        tile.addEducationLevel(60);
        service.spreadEffect(tile, 1, 30, true);
    }
}
