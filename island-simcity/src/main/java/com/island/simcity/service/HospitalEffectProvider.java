package com.island.simcity.service;

import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityTile;
import org.springframework.stereotype.Component;

@Component
public class HospitalEffectProvider implements SocialEffectProvider {
    @Override
    public BuildingComponent.Type getSupportedType() {
        return BuildingComponent.Type.HOSPITAL;
    }

    @Override
    public void applyEffect(CityTile tile, SocialService service) {
        tile.addHealthLevel(80);
        service.spreadEffect(tile, 2, 40, false);
    }
}
