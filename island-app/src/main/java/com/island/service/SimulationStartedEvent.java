package com.island.service;

import com.island.engine.core.SimulationContext;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a new simulation context has been created and started.
 */
public class SimulationStartedEvent extends ApplicationEvent {

    public SimulationStartedEvent(SimulationContext<?> source) {
        super(source);
    }

    public SimulationContext<?> getContext() {
        return (SimulationContext<?>) getSource();
    }
}
