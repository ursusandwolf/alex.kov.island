package com.island.nature.service;

import com.island.engine.event.AnimalDiedEvent;
import com.island.engine.event.EventBus;
import lombok.extern.slf4j.Slf4j;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Animal;

/**
 * Service that monitors simulation events and logs significant occurrences.
 */
@Slf4j
public class AlertService {

    public void subscribe(EventBus eventBus) {
        eventBus.subscribe(AnimalDiedEvent.class, this::handleAnimalDied);
    }

    private void handleAnimalDied(AnimalDiedEvent event) {
        Animal animal = event.getAnimal();
        if (event.getCause() == DeathCause.HUNGER) {
            log.debug("Animal {} died of {}", animal.getTypeName(), DeathCause.HUNGER.getDisplayName());
        }
    }
}