package com.island.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.island.nature.model.IslandSnapshot;
import com.island.simcity.model.CitySnapshot;

/**
 * Jackson Mixin to support polymorphic serialization of WorldSnapshot.
 * This keeps the engine module free from Jackson dependencies.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "simulationType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = IslandSnapshot.class, name = "nature"),
    @JsonSubTypes.Type(value = CitySnapshot.class, name = "simcity")
})
public interface WorldSnapshotMixin {
}
