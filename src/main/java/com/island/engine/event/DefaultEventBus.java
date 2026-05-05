package com.island.engine.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultEventBus implements EventBus {
    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(Object event) {
        Class<?> eventType = event.getClass();
        List<Consumer<?>> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers == null) {
            return;
        }

        for (Consumer<?> subscriber : eventSubscribers) {
            try {
                @SuppressWarnings("unchecked")
                Consumer<Object> consumer = (Consumer<Object>) subscriber;
                consumer.accept(event);
            } catch (Exception e) {
                log.error("EventBus subscriber threw exception for event {}: {}",
                        eventType.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public <E> void subscribe(Class<E> eventType, Consumer<E> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }
}
