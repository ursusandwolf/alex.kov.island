package com.island.engine.scheduling;

import com.island.engine.core.EngineAPI;
import com.island.engine.core.ExecutionMode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.internal.PhaseScheduler;
import com.island.engine.model.Mortal;
import com.island.engine.model.Tickable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates simulation ticks and manages task execution.
 * 
 * <p>The GameLoop is the "heartbeat" of the simulation. It maintains a constant tick rate 
 * and executes both built-in world logic and registered {@link ScheduledTask}s 
 * through several phases (PREPARE, SIMULATION, POSTPROCESS).</p>
 * 
 * <p>Thread Safety: This class is thread-safe. Tasks can be added from any thread 
 * while the loop is running. Modifications to the task list take effect at the beginning 
 * of the next tick.</p>
 * 
 * @param <T> The base type of entities in the simulation world.
 * @since 1.0
 */
@EngineAPI
@Slf4j
@RequiredArgsConstructor
public class GameLoop<T extends Mortal> {
    
    private final List<ScheduledTask> recurringTasks = new ArrayList<>();
    private final Queue<ScheduledTask> pendingTasks = new ConcurrentLinkedQueue<>();
    
    private final long tickDurationMs;
    @Getter
    private final ExecutorService taskExecutor;
    private final PhaseScheduler<T> scheduler;

    /**
     * The world instance to be ticked. 
     * Must be set before calling {@link #start()}.
     */
    @Getter @Setter
    private volatile SimulationWorld<T> world;
    
    /**
     * A predicate checked at the end of every tick. 
     * If it returns {@code true}, the loop stops automatically.
     */
    @Getter @Setter
    private volatile BooleanSupplier stopCondition;

    /**
     * An optional callback executed once the loop stops (either manually or via stopCondition).
     */
    @Setter
    private volatile Runnable onStopCallback;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public enum SimulationStatus {
        IDLE, RUNNING, PAUSED
    }

    public SimulationStatus getStatus() {
        if (!running.get()) return SimulationStatus.IDLE;
        return paused.get() ? SimulationStatus.PAUSED : SimulationStatus.RUNNING;
    }

    public void pause() {
        if (running.get()) {
            paused.set(true);
            log.info("GameLoop paused.");
        }
    }

    public void resume() {
        if (running.get()) {
            paused.set(false);
            log.info("GameLoop resumed.");
        }
    }

    /**
     * The total number of ticks executed since the loop started.
     */
    @Getter
    private int tickCount = 0;
    private long tasksVersion = 0;
    
    private volatile Future<?> loopTask;

    /**
     * Adds a specialized task to the recurring execution list.
     * 
     * @param task the task to register.
     */
    public void addRecurringTask(ScheduledTask task) {
        pendingTasks.add(task);
    }

    /**
     * Wraps a simple {@link Tickable} as a recurring simulation task.
     * The task will run in the {@link Phase#SIMULATION} phase.
     * 
     * @param task the logic to execute every tick.
     */
    public void addRecurringTask(Tickable task) {
        addRecurringTask(new TickableTaskWrapper(task));
    }

    /**
     * Wraps a {@link Runnable} as a recurring simulation task.
     * The task will run in the {@link Phase#SIMULATION} phase.
     * 
     * @param runnable the logic to execute every tick.
     */
    public void addRecurringTask(Runnable runnable) {
        addRecurringTask((Tickable) tc -> runnable.run());
    }

    /**
     * Starts the simulation loop in a background thread provided by the executor.
     * Does nothing if already running.
     * 
     * @throws IllegalStateException if the world is not set.
     */
    public void start() {
        if (world == null) {
            throw new IllegalStateException("Cannot start GameLoop: World is not set.");
        }
        if (running.compareAndSet(false, true)) {
            loopTask = taskExecutor.submit(this::run);
            log.info("GameLoop started.");
        }
    }

    /**
     * Requests the loop to stop. 
     * The current tick will be completed before the background thread terminates.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping GameLoop...");
            if (loopTask != null) {
                loopTask.cancel(true);
            }
        }
    }

    /**
     * Checks if the simulation loop is currently active.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Executes a single simulation tick synchronously.
     * 
     * <p>This method performs:
     * <ol>
     *     <li>Pending task registration</li>
     *     <li>World state tick (topology, cleanup)</li>
     *     <li>Phased task execution (PREPARE -> SIMULATION -> POSTPROCESS)</li>
     * </ol>
     * </p>
     */
    public void runTick() {
        tickCount++;
        
        // Drain pending tasks into the main list
        ScheduledTask pending;
        boolean tasksChanged = false;
        while ((pending = pendingTasks.poll()) != null) {
            recurringTasks.add(pending);
            tasksChanged = true;
        }
        
        if (tasksChanged) {
            tasksVersion++;
        }

        if (world != null) {
            try {
                world.tick(tickCount);
            } catch (Exception e) {
                log.error("Error during world tick: {}", e.getMessage(), e);
            }
        }

        try {
            scheduler.execute(world, recurringTasks, tickCount, tasksVersion);
        } catch (Exception e) {
            log.error("Error during simulation tick at step {}: {}", tickCount, e.getMessage(), e);
        }
    }

    private void run() {
        while (running.get()) {
            if (paused.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tickDurationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            long startTime = System.nanoTime();
            try {
                runTick();
                if (stopCondition != null && stopCondition.getAsBoolean()) {
                    log.info("Stop condition met. Stopping GameLoop.");
                    running.set(false);
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
                    // Exit loop gracefully on interruption
                    break;
                }
            }
        }
    }

    private record TickableTaskWrapper(Tickable delegate) implements ScheduledTask {
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
            delegate.tick(tickCount);
        }
    }
}