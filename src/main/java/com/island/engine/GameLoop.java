package com.island.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the simulation ticks.
 * Coordinator that manages the main simulation loop and delegates task execution.
 *
 * @param <T> The base type of entities in the simulation world.
 */
public class GameLoop<T extends Mortal> {
    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);
    
    private final List<ScheduledTask> recurringTasks = new ArrayList<>();
    private final Queue<ScheduledTask> pendingTasks = new ConcurrentLinkedQueue<>();
    private final long tickDurationMs;
    private final ExecutorService taskExecutor;
    private final PhaseScheduler<T> scheduler;
    private volatile boolean running = false;
    private int tickCount = 0;
    private SimulationWorld<T> world;
    private volatile Thread loopThread;

    public GameLoop(long tickDurationMs, int threadCount) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = (threadCount > 0)
                ? Executors.newFixedThreadPool(threadCount)
                : Executors.newVirtualThreadPerTaskExecutor();
        
        ParallelDispatcher<T> dispatcher = new ParallelDispatcher<>(taskExecutor);
        this.scheduler = new PhaseScheduler<>(dispatcher);
    }

    public void setWorld(SimulationWorld<T> world) {
        this.world = world;
    }

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
        if (loopThread != null) {
            loopThread.interrupt();
            try {
                loopThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        taskExecutor.shutdownNow();
    }

    public boolean isRunning() {
        return running;
    }

    public int getTickCount() {
        return tickCount;
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
            long startTime = System.currentTimeMillis();
            try {
                runTick();
            } catch (Throwable t) {
                log.error("CRITICAL ERROR: Simulation loop crashed: {}", t.getMessage(), t);
            }
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = tickDurationMs - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }
}
