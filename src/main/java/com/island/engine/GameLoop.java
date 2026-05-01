package com.island.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the simulation ticks.
 */
public class GameLoop {
    private final List<Tickable> recurringTasks = new ArrayList<>();
    private final long tickDurationMs;
    private final ExecutorService taskExecutor;
    private volatile boolean running = false;
    private int tickCount = 0;
    private SimulationWorld world;

    public GameLoop(long tickDurationMs, int threadCount) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = Executors.newFixedThreadPool(threadCount);
    }

    public void setWorld(SimulationWorld world) {
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
        new Thread(this::run).start();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        taskExecutor.shutdown();
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

    private void executeGroup(List<Tickable> group, boolean isCellServiceGroup) {
        if (isCellServiceGroup && world != null && !taskExecutor.isShutdown()) {
            runCellServicesParallel(group.stream().map(t -> (CellService) t).toList());
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

    private void runCellServicesParallel(List<CellService> services) {
        for (CellService service : services) {
            service.beforeTick(tickCount);
        }

        java.util.Collection<? extends java.util.Collection<? extends SimulationNode>> workUnits = world.getParallelWorkUnits();
        List<java.util.concurrent.Callable<SimulationMetrics>> tasks = new ArrayList<>();

        for (java.util.Collection<? extends SimulationNode> unit : workUnits) {
            tasks.add(() -> {
                long totalCurrent = 0;
                long totalMax = 0;
                int animalCount = 0;
                int starvingCount = 0;
                final long[] nodeStats = new long[4];

                for (SimulationNode node : unit) {
                    for (CellService service : services) {
                        service.processCell(node, tickCount);
                    }
                    
                    nodeStats[0] = 0;
                    nodeStats[1] = 0;
                    nodeStats[2] = 0;
                    nodeStats[3] = 0;
                    node.forEachAnimal(a -> {
                        if (a.isAlive()) {
                            nodeStats[0] += a.getCurrentEnergy();
                            nodeStats[1] += a.getMaxEnergy();
                            nodeStats[2]++;
                            if (a.isStarving()) {
                                nodeStats[3]++;
                            }
                        }
                    });
                    totalCurrent += nodeStats[0];
                    totalMax += nodeStats[1];
                    animalCount += (int) nodeStats[2];
                    starvingCount += (int) nodeStats[3];
                }
                return SimulationMetrics.builder()
                        .totalCurrentEnergy(totalCurrent)
                        .totalMaxEnergy(totalMax)
                        .animalCount(animalCount)
                        .starvingCount(starvingCount)
                        .build();
            });
        }

        try {
            List<java.util.concurrent.Future<SimulationMetrics>> futures = taskExecutor.invokeAll(tasks);
            SimulationMetrics totalMetrics = SimulationMetrics.empty();
            for (java.util.concurrent.Future<SimulationMetrics> future : futures) {
                totalMetrics = SimulationMetrics.combine(totalMetrics, future.get());
            }
            if (world.getStatisticsService() != null) {
                world.getStatisticsService().updateMetrics(totalMetrics);
            }
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Ignore if shutting down
        }

        for (CellService service : services) {
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
