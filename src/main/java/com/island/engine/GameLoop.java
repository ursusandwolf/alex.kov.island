package com.island.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final List<ScheduledTask> recurringTasks = new ArrayList<>();
    private final long tickDurationMs;
    private final ExecutorService taskExecutor;
    private volatile boolean running = false;
    private int tickCount = 0;
    private SimulationWorld<T> world;
    private Thread loopThread;

    public GameLoop(long tickDurationMs, int threadCount) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    }

    public void setWorld(SimulationWorld<T> world) {
        this.world = world;
    }

    public void addRecurringTask(Tickable task) {
        if (task instanceof ScheduledTask st) {
            recurringTasks.add(st);
        } else {
            recurringTasks.add(new ScheduledTask() {
                @Override
                public Phase phase() {
                    return Phase.SIMULATION;
                }

                @Override
                public int priority() {
                    return 50;
                }

                @Override
                public boolean isParallelizable() {
                    return false;
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
        
        if (world != null) {
            try {
                world.tick(tickCount);
            } catch (Exception e) {
                System.err.println("Error during world tick: " + e.getMessage());
                e.printStackTrace();
            }
        }

        Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
        for (Phase phase : Phase.values()) {
            phasedTasks.put(phase, new ArrayList<>());
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

            List<CellService<T, SimulationNode<T>>> parallelGroup = new ArrayList<>();
            for (ScheduledTask task : tasks) {
                if (task.isParallelizable() && task instanceof CellService) {
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
                        System.err.println("Error during simulation tick: " + e.getMessage());
                        e.printStackTrace();
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
            List<java.util.concurrent.Future<Void>> futures = taskExecutor.invokeAll(tasks);
            for (java.util.concurrent.Future<Void> future : futures) {
                try {
                    future.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    System.err.println("Error in parallel cell service execution: " + e.getCause().getMessage());
                    e.getCause().printStackTrace();
                }
            }
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
