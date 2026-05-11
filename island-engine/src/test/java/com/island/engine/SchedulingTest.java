package com.island.engine;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.core.SimulationWorld;
import com.island.engine.event.EventBus;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.Phase;
import com.island.engine.scheduling.ScheduledTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SchedulingTest {

    @Test
    @DisplayName("Scheduling Integration: Task ordering and execution through SimulationEngine")
    void scheduling_integration_test() {
        SimulationEngine<Mortal> engine = new SimulationEngine<>();
        AtomicInteger counter = new AtomicInteger(0);
        List<String> executionLog = new ArrayList<>();

        SimulationPlugin<Mortal> plugin = new SimulationPlugin<>() {
            @Override
            public SimulationWorld<Mortal> createWorld(EventBus eventBus) {
                SimulationWorld<Mortal> world = mock(SimulationWorld.class);
                return world;
            }

            @Override
            public void registerTasks(GameLoop<Mortal> gameLoop, SimulationWorld<Mortal> world, EventBus eventBus) {
                // Task 2: Higher priority in SIMULATION phase
                gameLoop.addRecurringTask(new ScheduledTask() {
                    @Override public Phase phase() { return Phase.SIMULATION; }
                    @Override public int priority() { return 100; }
                    @Override public void tick(int tc) { executionLog.add("Task 2 (High Prio SIM)"); }
                });

                // Task 3: Lower priority in SIMULATION phase
                gameLoop.addRecurringTask(new ScheduledTask() {
                    @Override public Phase phase() { return Phase.SIMULATION; }
                    @Override public int priority() { return 10; }
                    @Override public void tick(int tc) { executionLog.add("Task 3 (Low Prio SIM)"); }
                });

                // Task 1: PREPARE phase
                gameLoop.addRecurringTask(new ScheduledTask() {
                    @Override public Phase phase() { return Phase.PREPARE; }
                    @Override public int priority() { return 50; }
                    @Override public void tick(int tc) { executionLog.add("Task 1 (PREPARE)"); }
                });

                // Task 4: POSTPROCESS phase
                gameLoop.addRecurringTask(new ScheduledTask() {
                    @Override public Phase phase() { return Phase.POSTPROCESS; }
                    @Override public int priority() { return 50; }
                    @Override public void tick(int tc) { executionLog.add("Task 4 (POSTPROCESS)"); }
                });
            }
        };

        SimulationConfig config = SimulationConfig.defaultFor(1);
        try (SimulationContext<Mortal> context = engine.build(plugin, config)) {
            context.gameLoop().runTick();
        }

        // Verify correct execution order: PREPARE -> SIMULATION (ordered by priority) -> POSTPROCESS
        assertEquals(List.of(
                "Task 1 (PREPARE)",
                "Task 2 (High Prio SIM)",
                "Task 3 (Low Prio SIM)",
                "Task 4 (POSTPROCESS)"
        ), executionLog);
    }
}
