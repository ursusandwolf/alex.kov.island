package com.island.util.math;

import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.Mortal;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.jupiter.api.Assertions.*;

class GridUtilsTest {

    @Test
    void testIsValid() {
        assertTrue(GridUtils.isValid(0, 0, 10, 10));
        assertTrue(GridUtils.isValid(9, 9, 10, 10));
        assertFalse(GridUtils.isValid(-1, 0, 10, 10));
        assertFalse(GridUtils.isValid(0, -1, 10, 10));
        assertFalse(GridUtils.isValid(10, 0, 10, 10));
        assertFalse(GridUtils.isValid(0, 10, 10, 10));
    }

    @Test
    void testExecuteWithDoubleLockSameNode() {
        DummyNode node = new DummyNode();
        AtomicBoolean executed = new AtomicBoolean(false);

        GridUtils.executeWithDoubleLock(node, node, () -> executed.set(true));

        assertTrue(executed.get(), "Action should be executed");
        assertFalse(((ReentrantLock)node.getLock()).isLocked(), "Lock should be released");
    }

    @Test
    void testExecuteWithDoubleLockDifferentNodes() {
        DummyNode node1 = new DummyNode();
        DummyNode node2 = new DummyNode();
        AtomicBoolean executed = new AtomicBoolean(false);

        GridUtils.executeWithDoubleLock(node1, node2, () -> {
            assertTrue(((ReentrantLock)node1.getLock()).isLocked());
            assertTrue(((ReentrantLock)node2.getLock()).isLocked());
            executed.set(true);
        });

        assertTrue(executed.get(), "Action should be executed");
        assertFalse(((ReentrantLock)node1.getLock()).isLocked(), "Lock should be released");
        assertFalse(((ReentrantLock)node2.getLock()).isLocked(), "Lock should be released");
    }

    static class DummyNode implements SimulationNode<Mortal> {
        private final ReentrantLock lock = new ReentrantLock();

        @Override public SimulationWorld<Mortal> getWorld() { return null; }
        @Override public Lock getLock() { return lock; }
        @Override public String getCoordinates() { return "0,0"; }
        @Override public void setNeighbors(List<SimulationNode<Mortal>> neighbors) {}
        @Override public List<SimulationNode<Mortal>> getNeighbors() { return List.of(); }
        @Override public List<Mortal> getEntities() { return List.of(); }
        @Override public void forEachEntity(java.util.function.Consumer<Mortal> action) {}
        @Override public void query(com.island.engine.ecs.EntityQuery<Mortal> query, java.util.function.Consumer<Mortal> action) {}
        @Override public int getEntityCount() { return 0; }
        @Override public boolean canAccept(Mortal entity) { return false; }
        @Override public boolean addEntity(Mortal entity) { return false; }
        @Override public boolean removeEntity(Mortal entity) { return false; }
        @Override public void cleanupDeadEntities(java.util.function.Consumer<Mortal> onOrganismRemoved) {}
    }
}
