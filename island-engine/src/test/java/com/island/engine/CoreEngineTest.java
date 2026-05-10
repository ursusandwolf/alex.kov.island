package com.island.engine;

import com.island.engine.internal.EntityIdManager;
import com.island.engine.internal.DefaultEventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CoreEngineTest {

    @Test
    @DisplayName("EntityIdManager: ID acquisition and recycling")
    void id_manager_test() {
        EntityIdManager manager = new EntityIdManager();
        int id1 = manager.acquireId();
        int id2 = manager.acquireId();
        
        assertNotEquals(id1, id2);
        
        manager.releaseId(id1);
        int id3 = manager.acquireId();
        
        assertEquals(id1, id3, "Released ID should be recycled");
    }

    @Test
    @DisplayName("DefaultEventBus: Publish and Subscribe")
    void event_bus_test() {
        DefaultEventBus bus = new DefaultEventBus();
        AtomicInteger receivedCount = new AtomicInteger(0);
        
        bus.subscribe(String.class, msg -> {
            receivedCount.incrementAndGet();
        });
        
        bus.publish("Hello");
        bus.publish("World");
        bus.publish(123); // Should not increment receivedCount
        
        assertEquals(2, receivedCount.get());
    }
}
