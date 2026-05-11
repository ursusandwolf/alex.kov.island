package com.island.nature.model;

import com.island.engine.core.SimulationNode;
import com.island.engine.core.WorkUnit;
import com.island.nature.entities.core.Organism;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Composite: Chunk consists of cells. Implements WorkUnit for engine compatibility and instrumentation.
 */
public class Chunk implements WorkUnit<Organism> {
    private final int chunkIdX;
    private final int chunkIdY;
    private final int startX;
    private final int endX;
    private final int startY;
    private final int endY;
    private final Island island;
    private final List<Cell> cells = new ArrayList<>();
    private long lastExecutionTimeNanos;

    public Chunk(int idX, int idY, int sx, int ex, int sy, int ey, Island island) {
        this.chunkIdX = idX;
        this.chunkIdY = idY;
        this.startX = sx;
        this.endX = ex;
        this.startY = sy;
        this.endY = ey;
        this.island = island;
        initCells();
    }

    private void initCells() {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                cells.add(island.getGrid()[x][y]);
            }
        }
    }

    public List<Cell> getCells() {
        return cells;
    }

    public int getChunkIdX() {
        return chunkIdX;
    }

    public int getChunkIdY() {
        return chunkIdY;
    }

    @Override
    public String toString() {
        return String.format("Chunk[%d,%d] cells=%d time=%dms", chunkIdX, chunkIdY, 
                cells.size(), lastExecutionTimeNanos / 1000000);
    }

    // Collection implementation delegating to cells
    @Override
    public int size() {
        return cells.size();
    }

    @Override
    public boolean isEmpty() {
        return cells.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return cells.contains(o);
    }

    @Override
    public Iterator<SimulationNode<Organism>> iterator() {
        return (Iterator) cells.iterator();
    }

    @Override
    public Object[] toArray() {
        return cells.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return cells.toArray(a);
    }

    @Override
    public boolean add(SimulationNode<Organism> node) {
        if (node instanceof Cell cell) {
            return cells.add(cell);
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return cells.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return cells.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends SimulationNode<Organism>> c) {
        boolean changed = false;
        for (SimulationNode<Organism> node : c) {
            if (add(node)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return cells.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return cells.retainAll(c);
    }

    @Override
    public void clear() {
        cells.clear();
    }

    public long getLastExecutionTimeNanos() {
        return lastExecutionTimeNanos;
    }

    public void setLastExecutionTimeNanos(long lastExecutionTimeNanos) {
        this.lastExecutionTimeNanos = lastExecutionTimeNanos;
    }
}
