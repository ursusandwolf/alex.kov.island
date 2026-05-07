package com.island.engine.event;

import com.island.nature.entities.core.Animal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when a new animal is born.
 */
@Getter
@RequiredArgsConstructor
public class AnimalBornEvent {
    private final Animal animal;
}
