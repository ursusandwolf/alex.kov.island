package com.island.simcity.service;

import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityTile;
import org.springframework.stereotype.Component;

@Component
public class ParkEffectProvider implements SocialEffectProvider {
    @Override
    public BuildingComponent.Type getSupportedType() {
        return BuildingComponent.Type.PARK;
    }

    @Override
    public void applyEffect(CityTile tile, SocialService service) {
        tile.addHealthLevel(10);
        tile.setDesirability(tile.getDesirability() + 15);
    }
}
