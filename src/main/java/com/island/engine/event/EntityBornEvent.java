package com.island.engine.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.island.engine.model.Mortal;

/**
 * Event published when a new entity (Mortal) is born in the simulation.
 */
@Getter
@RequiredArgsConstructor
public class EntityBornEvent {
    private final Mortal entity;
}