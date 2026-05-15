package com.island.simcity.service;

import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityTile;
import org.springframework.stereotype.Component;

@Component
public class CollegeEffectProvider implements SocialEffectProvider {
    @Override
    public BuildingComponent.Type getSupportedType() {
        return BuildingComponent.Type.COLLEGE;
    }

    @Override
    public void applyEffect(CityTile tile, SocialService service) {
        tile.addEducationLevel(100);
        service.spreadEffect(tile, 2, 50, true);
    }
}
