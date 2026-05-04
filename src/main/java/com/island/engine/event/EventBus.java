package com.island.engine.event;

import java.util.function.Consumer;

/**
 * A thread-safe event bus for decoupled communication between simulation components.
 */
public interface EventBus {
    /**
     * Publishes an event to all registered subscribers.
     * @param event The event object to publish.
     */
    void publish(Object event);

    /**
     * Registers a subscriber for a specific event type.
     * @param eventType The class of the event to listen for.
     * @param subscriber The callback to execute when the event is published.
     * @param <E> The event type.
     */
    <E> void subscribe(Class<E> eventType, Consumer<E> subscriber);
}
