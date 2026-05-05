package com.island.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles parallel execution of CellServices across world work units.
 *
 * @param <T> The base type of entities.
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelDispatcher<T extends Mortal> {

    private final List<CellProcessor<T>> processorPool = new ArrayList<>();
    private final ExecutorService taskExecutor;

    public void dispatch(SimulationWorld<T> world, List<CellService<T, SimulationNode<T>>> services, int tickCount) {
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
                    log.error("Task execution rejected in parallel dispatcher: {}. Executing synchronously.", e.getMessage());
                    processor.run(); // Fallback to synchronous execution
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
        private volatile Collection<? extends SimulationNode<T>> unit;
        private volatile List<CellService<T, SimulationNode<T>>> services;
        private volatile int tickCount;
        private volatile CountDownLatch latch;
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
}
