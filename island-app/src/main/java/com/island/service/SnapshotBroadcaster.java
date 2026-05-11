package com.island.service;

import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.Mortal;
import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.Phase;
import com.island.engine.scheduling.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Task that creates a world snapshot and broadcasts it via WebSockets.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotBroadcaster implements ScheduledTask {

    private final SimpMessagingTemplate messagingTemplate;
    private SimulationContext<Mortal> currentContext;

    public void setContext(SimulationContext<Mortal> context) {
        this.currentContext = context;
    }

    @Override
    public Phase phase() {
        return Phase.POSTPROCESS;
    }

    @Override
    public int priority() {
        return 0; // Low priority, after other post-processing
    }

    @Override
    public void tick(int tickCount) {
        if (currentContext == null) return;

        // Broadcast every 2 ticks to reduce traffic, or adjust as needed
        if (tickCount % 2 == 0) {
            SimulationWorld<Mortal> world = currentContext.world();
            if (world != null) {
                WorldSnapshot snapshot = world.createSnapshot();
                messagingTemplate.convertAndSend("/topic/snapshot", snapshot);
                log.trace("Broadcasted snapshot for tick {}", tickCount);
            }
        }
    }
}
