package com.island.nature.service;

import com.island.engine.event.EntityDiedEvent;
import com.island.engine.event.EventBus;
import lombok.extern.slf4j.Slf4j;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;

/**
 * Service that monitors simulation events and logs significant occurrences.
 */
@Slf4j
public class AlertService {

    public void subscribe(EventBus eventBus) {
        eventBus.subscribe(EntityDiedEvent.class, this::handleEntityDied);
    }

    private void handleEntityDied(EntityDiedEvent event) {
        if (event.getEntity() instanceof Organism organism) {
            // Log if something important happens, e.g., mass death or specific species dying out
            // For now, just log all deaths at debug level or significant ones at info
            if (DeathCause.HUNGER.name().equals(event.getCause())) {
                log.debug("Organism {} died of {}", organism.getTypeName(), DeathCause.HUNGER.getDisplayName());
            }
        }
    }
}