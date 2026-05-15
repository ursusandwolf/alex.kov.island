package com.island.simcity.service;

import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityTile;

/**
 * Strategy interface for social building effects.
 */
public interface SocialEffectProvider {
    BuildingComponent.Type getSupportedType();
    void applyEffect(CityTile tile, SocialService service);
}
