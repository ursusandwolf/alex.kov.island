package com.island.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the simulation ticks.
 * Generic engine component that schedules and executes simulation tasks.
 *
 * @param <T> The base type of entities in the simulation world.
 */
public class GameLoop<T extends Mortal> {
    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);
    
    private final List<ScheduledTask> recurringTasks = new ArrayList<>();
    private final Queue<ScheduledTask> pendingTasks = new ConcurrentLinkedQueue<>();
    private final long tickDurationMs;
    private final ExecutorService taskExecutor;
    private volatile boolean running = false;
    private int tickCount = 0;
    private SimulationWorld<T, ?> world;
    private Thread loopThread;

    // Fixed structures to reduce allocations
    private final Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
    private final List<CellService<T, SimulationNode<T>>> parallelGroup = new ArrayList<>();
    private final List<CellProcessor<T>> processorPool = new ArrayList<>();

    public GameLoop(long tickDurationMs, int threadCount) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = (threadCount > 0)
                ? Executors.newFixedThreadPool(threadCount)
                : Executors.newVirtualThreadPerTaskExecutor();
        
        for (Phase phase : Phase.values()) {
            phasedTasks.put(phase, new ArrayList<>());
        }
    }

    public void setWorld(SimulationWorld<T, ?> world) {
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
        loopThread = new Thread(this::run);
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

        // Reuse the fixed structure for phases to reduce allocations
        for (List<ScheduledTask> list : phasedTasks.values()) {
            list.clear();
        }

        for (ScheduledTask task : recurringTasks) {
            phasedTasks.get(task.phase()).add(task);
        }

        for (Phase phase : Phase.values()) {
            List<ScheduledTask> tasks = phasedTasks.get(phase);
            if (tasks.isEmpty()) {
                continue;
            }

            // Sort descending by priority (higher priority executes first)
            tasks.sort(Comparator.comparingInt(ScheduledTask::priority).reversed());

            parallelGroup.clear();
            for (ScheduledTask task : tasks) {
                if (task.executionMode() == ExecutionMode.PARALLEL && task instanceof CellService) {
                    @SuppressWarnings("unchecked")
                    CellService<T, SimulationNode<T>> cellService = (CellService<T, SimulationNode<T>>) task;
                    parallelGroup.add(cellService);
                } else {
                    if (!parallelGroup.isEmpty()) {
                        runCellServicesParallel(parallelGroup);
                        parallelGroup.clear();
                    }
                    try {
                        task.tick(tickCount);
                    } catch (Exception e) {
                        log.error("Error during simulation tick in phase {}: {}", phase, e.getMessage(), e);
                    }
                }
            }
            
            if (!parallelGroup.isEmpty()) {
                runCellServicesParallel(parallelGroup);
                parallelGroup.clear();
            }
        }
    }

    private void runCellServicesParallel(List<CellService<T, SimulationNode<T>>> services) {
        if (world == null || taskExecutor.isShutdown()) {
            return;
        }

        for (CellService<T, SimulationNode<T>> service : services) {
            try {
                service.beforeTick(tickCount);
            } catch (Exception e) {
                log.error("Error in beforeTick for service {}: {}", service.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        Collection<? extends Collection<? extends SimulationNode<T>>> workUnits = world.getParallelWorkUnits();
        int unitCount = workUnits.size();
        
        if (unitCount > 0) {
            // Ensure pool capacity
            while (processorPool.size() < unitCount) {
                processorPool.add(new CellProcessor<>());
            }

            CountDownLatch latch = new CountDownLatch(unitCount);
            int i = 0;
            for (Collection<? extends SimulationNode<T>> unit : workUnits) {
                CellProcessor<T> processor = processorPool.get(i++);
                processor.update(unit, services, tickCount, latch);
                try {
                    taskExecutor.execute(processor);
                } catch (RejectedExecutionException e) {
                    latch.countDown();
                }
            }

            try {
                latch.await();
                // Check for critical errors
                for (int j = 0; j < unitCount; j++) {
                    Throwable error = processorPool.get(j).getError();
                    if (error != null) {
                        log.error("Critical error in parallel cell service execution: {}", error.getMessage(), error);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (CellService<T, SimulationNode<T>> service : services) {
            try {
                service.afterTick(tickCount);
            } catch (Exception e) {
                log.error("Error in afterTick for service {}: {}", service.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    private static class CellProcessor<T extends Mortal> implements Runnable {
        private Collection<? extends SimulationNode<T>> unit;
        private List<CellService<T, SimulationNode<T>>> services;
        private int tickCount;
        private CountDownLatch latch;
        private volatile Throwable error;

        void update(Collection<? extends SimulationNode<T>> unit, List<CellService<T, SimulationNode<T>>> services, int tickCount, CountDownLatch latch) {
            this.unit = unit;
            this.services = services;
            this.tickCount = tickCount;
            this.latch = latch;
            this.error = null;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public void run() {
            try {
                for (SimulationNode<T> node : unit) {
                    for (CellService<T, SimulationNode<T>> service : services) {
                        try {
                            service.processCell(node, tickCount);
                        } catch (Exception e) {
                            log.error("Error processing cell {} in service {}: {}", 
                                    node.getCoordinates(), service.getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }
                }
            } catch (Throwable t) {
                this.error = t;
            } finally {
                latch.countDown();
            }
        }
    }

    private void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            try {
                runTick();
            } catch (Throwable t) {
                log.error("CRITICAL ERROR: Simulation loop crashed: {}", t.getMessage(), t);
                // We might want to stop the simulation if a critical error occurs repeatedly
                // but for now, we just log and continue to next tick if possible.
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
