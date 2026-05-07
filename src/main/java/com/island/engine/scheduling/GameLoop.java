package com.island.engine.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.island.engine.core.ExecutionMode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.Mortal;
import com.island.engine.model.Tickable;

/**
 * Orchestrates the simulation ticks.
 * Coordinator that manages the main simulation loop and delegates task execution.
 *
 * @param <T> The base type of entities in the simulation world.
 */
@Slf4j
@RequiredArgsConstructor
public class GameLoop<T extends Mortal> {
    
    private final List<ScheduledTask> recurringTasks = new ArrayList<>();
    private final Queue<ScheduledTask> pendingTasks = new ConcurrentLinkedQueue<>();
    
    private final long tickDurationMs;
    @Getter
    private final ExecutorService taskExecutor;
    private final PhaseScheduler<T> scheduler;

    @Getter @Setter
    private SimulationWorld<T> world;
    
    @Getter @Setter
    private java.util.function.Supplier<Boolean> stopCondition;

    @Setter
    private Runnable onStopCallback;

    @Getter
    private volatile boolean running = false;
    @Getter
    private int tickCount = 0;
    
    private volatile Thread loopThread;

    public void addRecurringTask(Tickable task) {
        if (task instanceof ScheduledTask st) {
            pendingTasks.add(st);
        } else {
            pendingTasks.add(new ScheduledTask() {
                @Override
                public Phase phase() {
                    return Phase.SIMULATION;
                }

                @Override
                public int priority() {
                    return 50;
                }

                @Override
                public ExecutionMode executionMode() {
                    return ExecutionMode.SEQUENTIAL;
                }

                @Override
                public void tick(int tickCount) {
                    task.tick(tickCount);
                }
            });
        }
    }

    public void addRecurringTask(Runnable runnable) {
        addRecurringTask((Tickable) tc -> runnable.run());
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        loopThread = new Thread(this::run, "GameLoopThread");
        loopThread.start();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (loopThread != null && Thread.currentThread() != loopThread) {
            loopThread.interrupt();
            try {
                loopThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        taskExecutor.shutdownNow();
    }

    public void runTick() {
        tickCount++;
        
        // Drain pending tasks into the main list
        ScheduledTask pending;
        while ((pending = pendingTasks.poll()) != null) {
            recurringTasks.add(pending);
        }

        if (world != null) {
            try {
                world.tick(tickCount);
            } catch (Exception e) {
                log.error("Error during world tick: {}", e.getMessage(), e);
            }
        }

        try {
            scheduler.execute(world, recurringTasks, tickCount);
        } catch (Exception e) {
            log.error("Error during simulation tick at step {}: {}", tickCount, e.getMessage(), e);
        }
    }

    private void run() {
        while (running) {
            long startTime = System.nanoTime();
            try {
                runTick();
                if (stopCondition != null && stopCondition.get()) {
                    log.info("Stop condition met. Stopping GameLoop.");
                    running = false;
                    if (onStopCallback != null) {
                        onStopCallback.run();
                    }
                    break;
                }
            } catch (Throwable t) {
                log.error("CRITICAL ERROR: Simulation loop crashed: {}", t.getMessage(), t);
            }
            long elapsed = System.nanoTime() - startTime;
            long sleepTimeNs = (tickDurationMs * 1_000_000) - elapsed;
            if (sleepTimeNs > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepTimeNs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}