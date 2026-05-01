package com.island.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Orchestrates the simulation ticks.
 * Generic engine component that schedules and executes simulation tasks.
 *
 * @param <T> The base type of entities in the simulation world.
 */
public class GameLoop<T extends Mortal> {
    private final List<Tickable> recurringTasks = new ArrayList<>();
    private final long tickDurationMs;
    private final ExecutorService taskExecutor;
    private volatile boolean running = false;
    private int tickCount = 0;
    private SimulationWorld<T> world;
    private Thread loopThread;

    public GameLoop(long tickDurationMs, int threadCount) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = Executors.newFixedThreadPool(threadCount);
    }

    public void setWorld(SimulationWorld<T> world) {
        this.world = world;
    }

    public void addRecurringTask(Tickable task) {
        recurringTasks.add(task);
    }

    public void addRecurringTask(Runnable runnable) {
        recurringTasks.add(t -> runnable.run());
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
        
        List<Tickable> currentGroup = new ArrayList<>();
        boolean inCellServiceGroup = false;

        for (Tickable task : recurringTasks) {
            boolean isCellService = task instanceof CellService;
            
            if (isCellService != inCellServiceGroup && !currentGroup.isEmpty()) {
                executeGroup(currentGroup, inCellServiceGroup);
                currentGroup.clear();
            }
            
            inCellServiceGroup = isCellService;
            currentGroup.add(task);
        }
        
        if (!currentGroup.isEmpty()) {
            executeGroup(currentGroup, inCellServiceGroup);
        }
    }

    @SuppressWarnings("unchecked")
    private void executeGroup(List<Tickable> group, boolean isCellServiceGroup) {
        if (isCellServiceGroup && world != null && !taskExecutor.isShutdown()) {
            runCellServicesParallel(group.stream().map(t -> (CellService<T, SimulationNode<T>>) t).toList());
        } else {
            for (Tickable task : group) {
                try {
                    task.tick(tickCount);
                } catch (Exception e) {
                    System.err.println("Error during simulation tick: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void runCellServicesParallel(List<CellService<T, SimulationNode<T>>> services) {
        for (CellService<T, SimulationNode<T>> service : services) {
            service.beforeTick(tickCount);
        }

        Collection<? extends Collection<? extends SimulationNode<T>>> workUnits = world.getParallelWorkUnits();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (Collection<? extends SimulationNode<T>> unit : workUnits) {
            tasks.add(() -> {
                for (SimulationNode<T> node : unit) {
                    for (CellService<T, SimulationNode<T>> service : services) {
                        service.processCell(node, tickCount);
                    }
                }
                return null;
            });
        }

        try {
            taskExecutor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {
            // Ignore if shutting down
        }

        for (CellService<T, SimulationNode<T>> service : services) {
            service.afterTick(tickCount);
        }
    }

    private void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            runTick();
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
