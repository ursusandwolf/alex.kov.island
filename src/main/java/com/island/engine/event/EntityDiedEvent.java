package com.island.engine.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.island.engine.model.Mortal;

/**
 * Event published when an entity (Mortal) dies in the simulation.
 */
@Getter
@RequiredArgsConstructor
public class EntityDiedEvent {
    private final Mortal entity;
    private final String cause;
}