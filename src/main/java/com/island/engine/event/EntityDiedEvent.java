package com.island.engine.event;

import com.island.engine.Mortal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when an entity (Mortal) dies in the simulation.
 */
@Getter
@RequiredArgsConstructor
public class EntityDiedEvent {
    private final Mortal entity;
    private final String cause;
}
