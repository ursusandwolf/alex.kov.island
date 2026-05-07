package com.island.engine.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.island.engine.model.Mortal;

/**
 * Default implementation of WorkUnit for simple use cases.
 */
public class DefaultWorkUnit<T extends Mortal> implements WorkUnit<T> {
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

    // Collection implementation delegating to nodes
    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    @Override
    public Iterator<SimulationNode<T>> iterator() {
        return nodes.iterator();
    }

    @Override
    public Object[] toArray() {
        return nodes.toArray();
    }

    @Override
    public <U> U[] toArray(U[] a) {
        return nodes.toArray(a);
    }

    @Override
    public boolean add(SimulationNode<T> node) {
        return nodes.add(node);
    }

    @Override
    public boolean remove(Object o) {
        return nodes.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return nodes.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends SimulationNode<T>> c) {
        return nodes.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return nodes.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return nodes.retainAll(c);
    }

    @Override
    public void clear() {
        nodes.clear();
    }
}
