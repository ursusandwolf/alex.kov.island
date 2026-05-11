package com.island.engine;

import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.core.WorkUnit;
import com.island.engine.model.Mortal;
import com.island.engine.event.EventBus;
import com.island.engine.model.WorldSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SimulationWorldTest {

    @Test
    @DisplayName("SimulationWorld: Initialization and node access")
    void world_test() {
        EventBus bus = EventBus.create();
        SimulationWorld<Mortal> world = new SimulationWorld<Mortal>() {
            @Override public Collection<? extends WorkUnit<Mortal>> getParallelWorkUnits() { return Collections.emptyList(); }
            @Override public Optional<SimulationNode<Mortal>> getNode(SimulationNode<Mortal> current, int dx, int dy) { return Optional.empty(); }
            @Override public boolean moveEntity(Mortal entity, SimulationNode<Mortal> from, SimulationNode<Mortal> to) { return false; }
            @Override public int getWidth() { return 2; }
            @Override public int getHeight() { return 2; }
            @Override public WorldSnapshot createSnapshot() { return null; }
            @Override public EventBus getEventBus() { return bus; }
            @Override public void tick(int tickCount) {}
        };
        
        world.initialize();
        assertNotNull(world);
        assertEquals(bus, world.getEventBus(), "World should return the assigned event bus");
    }
}
