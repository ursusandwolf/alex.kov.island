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
     * Supports type hierarchy: subscribing to a parent class/interface will
     * receive events of all subtypes. For example, subscribing to {@code Object.class}
     * acts as a wildcard and will receive ALL events.
     *
     * @param eventType The class of the event to listen for.
     * @param subscriber The callback to execute when the event is published.
     * @param <E> The event type.
     */
    <E> void subscribe(Class<E> eventType, Consumer<E> subscriber);

    /**
     * Unregisters a subscriber for a specific event type.
     * @param eventType The class of the event.
     * @param subscriber The subscriber to remove.
     * @param <E> The event type.
     */
    <E> void unsubscribe(Class<E> eventType, Consumer<E> subscriber);
}