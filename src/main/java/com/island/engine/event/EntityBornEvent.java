package com.island.engine.event;

import com.island.engine.Mortal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when a new entity (Mortal) is born in the simulation.
 */
@Getter
@RequiredArgsConstructor
public class EntityBornEvent {
    private final Mortal entity;
}
