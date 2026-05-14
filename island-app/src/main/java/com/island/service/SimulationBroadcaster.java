package com.island.service;

import com.island.config.SimulationProperties;
import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.Phase;
import com.island.engine.scheduling.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates broadcasting of simulation states and events via WebSockets.
 * Listens for SimulationStartedEvent to register itself in the game loop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationBroadcaster {

    private final SimpMessagingTemplate messaging;
    private final SimulationService simulationService;
    private final SimulationProperties properties;
    private final AtomicReference<WorldSnapshot> pending = new AtomicReference<>();

    private volatile Integer dynamicInterval;

    /**
     * Automatically registers the broadcaster as a recurring task in the simulation game loop
     * once a new simulation context is started.
     */
    @EventListener(SimulationStartedEvent.class)
    public void startBroadcasting(SimulationStartedEvent event) {
        event.getContext().gameLoop().addRecurringTask(new TickBroadcastTask());
        log.info("Simulation broadcasting attached to new context with interval: {} ticks", getSnapshotInterval());
    }

    private int getSnapshotInterval() {
        return dynamicInterval != null ? dynamicInterval : properties.getBroadcastInterval();
    }

    /**
     * Task that runs within the simulation thread to capture and broadcast snapshots.
     */
    private class TickBroadcastTask implements ScheduledTask {
        @Override
        public Phase phase() {
            return Phase.POSTPROCESS;
        }

        @Override
        public int priority() {
            return Integer.MIN_VALUE; // Run last in post-process
        }

        @Override
        public void tick(int tickCount) {
            if (tickCount % getSnapshotInterval() != 0) return;

            simulationService.getSnapshot().ifPresent(pending::set);
        }
    }

    @Scheduled(fixedRateString = "${sim.broadcast-rate-ms}")
    public void broadcast() {
        WorldSnapshot snapshot = pending.getAndSet(null);
        if (snapshot != null) {
            messaging.convertAndSend("/topic/world-state", snapshot);
            log.trace("Broadcasted world state from pending queue");
        }
    }

    /**
     * Updates the broadcast interval dynamically.
     * 
     * @param interval the new interval in ticks
     */
    public void setSnapshotInterval(int interval) {
        this.dynamicInterval = Math.max(1, interval);
        log.info("Broadcast interval updated to {} ticks", this.dynamicInterval);
    }
}
