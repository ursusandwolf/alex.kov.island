package com.island.engine.event;


import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    @Test
    void shouldPublishToDirectSubscribers() {
        EventBus bus = EventBus.create();
        AtomicInteger count = new AtomicInteger(0);
        bus.subscribe(String.class, s -> count.incrementAndGet());

        bus.publish("Hello");
        assertEquals(1, count.get());
    }

    @Test
    void shouldPublishToHierarchicalSubscribers() {
        EventBus bus = EventBus.create();
        AtomicInteger count = new AtomicInteger(0);
        bus.subscribe(Object.class, o -> count.incrementAndGet());
        bus.subscribe(CharSequence.class, cs -> count.incrementAndGet());

        bus.publish("Hello");
        // String is both Object and CharSequence
        assertEquals(2, count.get());
    }

    @Test
    void shouldUnsubscribeCorrectly() {
        EventBus bus = EventBus.create();
        AtomicInteger count = new AtomicInteger(0);
        java.util.function.Consumer<String> subscriber = s -> count.incrementAndGet();
        
        bus.subscribe(String.class, subscriber);
        bus.publish("One");
        assertEquals(1, count.get());

        bus.unsubscribe(String.class, subscriber);
        bus.publish("Two");
        assertEquals(1, count.get());
    }

    @Test
    void shouldHandleSubscribersThrowingExceptions() {
        EventBus bus = EventBus.create();
        AtomicInteger count = new AtomicInteger(0);
        
        bus.subscribe(String.class, s -> { throw new RuntimeException("Fail"); });
        bus.subscribe(String.class, s -> count.incrementAndGet());

        bus.publish("Hello");
        // Second subscriber should still run
        assertEquals(1, count.get());
    }
}