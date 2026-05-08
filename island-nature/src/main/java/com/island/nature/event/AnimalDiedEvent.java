package com.island.nature.event;

import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.DeathCause;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when an animal dies.
 */
@Getter
@RequiredArgsConstructor
public class AnimalDiedEvent {
    private final Animal animal;
    private final DeathCause cause;
}
