package com.island.nature.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the type of landscape in a cell.
 */
@Getter
@RequiredArgsConstructor
public enum TerrainType {
    MEADOW("Meadow", true, true),
    FOREST("Forest", true, true),
    WATER("Water", false, true),
    MOUNTAIN("Mountain", true, false);

    private final String displayName;
    private final boolean landAccessible;
    private final boolean waterAccessible;
}
