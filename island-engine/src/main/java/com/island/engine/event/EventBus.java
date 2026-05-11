package com.island.engine.event;

import com.island.engine.core.EngineAPI;
import java.util.function.Consumer;

/**
 * A thread-safe event bus for decoupled communication between simulation components.
 * 
 * <p>The event bus follows a publish-subscribe pattern. Subscribers are notified 
 * synchronously or asynchronously depending on the implementation, but the interface 
 * remains the same.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * EventBus bus = EventBus.create();
 * bus.subscribe(MyEvent.class, event -> System.out.println("Received: " + event));
 * bus.publish(new MyEvent("Hello World"));
 * }</pre>
 * </p>
 * 
 * @since 1.0
 */
@EngineAPI
public interface EventBus {
    /**
     * Creates a new instance of the default event bus implementation.
     * 
     * @return A new EventBus instance.
     */
    static EventBus create() {
        return new com.island.engine.internal.DefaultEventBus();
    }

    /**
     * Publishes an event to all registered subscribers.
     * 
     * @param event The event object to publish. Must not be null.
     */
    void publish(Object event);

    /**
     * Registers a subscriber for a specific event type.
     * Supports type hierarchy: subscribing to a parent class/interface will
     * receive events of all subtypes. For example, subscribing to {@code Object.class}
     * acts as a wildcard and will receive ALL events.
     *
     * @param eventType  The class of the event to listen for.
     * @param subscriber The callback to execute when the event is published.
     * @param <E>        The event type.
     */
    <E> void subscribe(Class<E> eventType, Consumer<E> subscriber);

    /**
     * Unregisters a subscriber for a specific event type.
     * 
     * @param eventType  The class of the event.
     * @param subscriber The subscriber to remove.
     * @param <E>        The event type.
     */
    <E> void unsubscribe(Class<E> eventType, Consumer<E> subscriber);
}