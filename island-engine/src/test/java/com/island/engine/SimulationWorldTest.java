package com.island.engine;

import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.core.WorkUnit;
import com.island.engine.event.EventBus;
import com.island.engine.model.Mortal;
import com.island.engine.model.WorldSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SimulationWorldTest {

    @Test
    @DisplayName("SimulationWorld: Lifecycle and EventBus integration")
    void world_lifecycle_test() {
        AtomicBoolean initCalled = new AtomicBoolean(false);
        EventBus bus = EventBus.create();
        
        SimulationWorld<Mortal> world = new SimulationWorld<Mortal>() {
            @Override public void initialize() { initCalled.set(true); }
            @Override public EventBus getEventBus() { return bus; }
            
            @Override public Collection<? extends WorkUnit<Mortal>> getParallelWorkUnits() { return Collections.emptyList(); }
            @Override public Optional<SimulationNode<Mortal>> getNode(SimulationNode<Mortal> curr, int dx, int dy) { return Optional.empty(); }
            @Override public boolean moveEntity(Mortal e, SimulationNode<Mortal> f, SimulationNode<Mortal> t) { return false; }
            @Override public int getWidth() { return 10; }
            @Override public int getHeight() { return 10; }
            @Override public WorldSnapshot createSnapshot() { return mock(WorldSnapshot.class); }
            @Override public void tick(int tc) {}
        };
        
        world.initialize();
        assertTrue(initCalled.get(), "initialize() should be called");
        assertEquals(bus, world.getEventBus(), "getEventBus() should return the assigned bus");
        assertEquals(10, world.getWidth());
        assertEquals(10, world.getHeight());
    }

    @Test
    @DisplayName("SimulationWorld: Default methods safety")
    void world_defaults_test() {
        SimulationWorld<Mortal> world = new SimulationWorld<Mortal>() {
            @Override public Collection<? extends WorkUnit<Mortal>> getParallelWorkUnits() { return null; }
            @Override public Optional<SimulationNode<Mortal>> getNode(SimulationNode<Mortal> c, int dx, int dy) { return null; }
            @Override public boolean moveEntity(Mortal e, SimulationNode<Mortal> f, SimulationNode<Mortal> t) { return false; }
            @Override public int getWidth() { return 0; }
            @Override public int getHeight() { return 0; }
            @Override public WorldSnapshot createSnapshot() { return null; }
            @Override public EventBus getEventBus() { return null; }
            @Override public void tick(int tc) {}
        };

        // These should not throw even with null/dummy state
        assertDoesNotThrow(world::initialize);
        assertDoesNotThrow(world::rebalance);
        assertDoesNotThrow(() -> world.onEntityAdded(null));
        assertDoesNotThrow(() -> world.onEntityRemoved(null));
    }

    // Helper to avoid full mockito dependency if possible, but it's already in dependencies
    private <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
