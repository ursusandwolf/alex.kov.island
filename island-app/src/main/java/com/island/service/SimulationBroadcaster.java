package com.island.service;

import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.Phase;
import com.island.engine.scheduling.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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

    @Value("${sim.broadcast-interval:5}")
    private int snapshotInterval;

    /**
     * Automatically registers the broadcaster as a recurring task in the simulation game loop
     * once a new simulation context is started.
     */
    @EventListener(SimulationStartedEvent.class)
    public void startBroadcasting(SimulationStartedEvent event) {
        event.getContext().gameLoop().addRecurringTask(new TickBroadcastTask());
        log.info("Simulation broadcasting attached to new context with interval: {} ticks", snapshotInterval);
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
            if (tickCount % snapshotInterval != 0) return;

            WorldSnapshot snapshot = simulationService.getSnapshot();
            if (snapshot != null) {
                messaging.convertAndSend("/topic/world-state", snapshot);
                log.trace("Broadcasted world state for tick {}", tickCount);
            }
        }
    }

    /**
     * Updates the broadcast interval dynamically.
     * 
     * @param interval the new interval in ticks
     */
    public void setSnapshotInterval(int interval) {
        this.snapshotInterval = Math.max(1, interval);
        log.info("Broadcast interval updated to {} ticks", this.snapshotInterval);
    }
}
