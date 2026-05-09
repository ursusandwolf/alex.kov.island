package com.island.engine.core;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.island.engine.model.Mortal;

/**
 * Default implementation of WorkUnit for simple use cases.
 */
@EngineAPI
public class DefaultWorkUnit<T extends Mortal> extends AbstractList<SimulationNode<T>> implements WorkUnit<T> {
    private final List<SimulationNode<T>> nodes;
    private long lastExecutionTimeNanos;

    public DefaultWorkUnit(Collection<? extends SimulationNode<T>> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    @Override
    public void setLastExecutionTimeNanos(long nanos) {
        this.lastExecutionTimeNanos = nanos;
    }

    @Override
    public long getLastExecutionTimeNanos() {
        return lastExecutionTimeNanos;
    }

    @Override
    public SimulationNode<T> get(int index) {
        return nodes.get(index);
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean add(SimulationNode<T> node) {
        return nodes.add(node);
    }

    @Override
    public SimulationNode<T> set(int index, SimulationNode<T> element) {
        return nodes.set(index, element);
    }

    @Override
    public SimulationNode<T> remove(int index) {
        return nodes.remove(index);
    }
}
