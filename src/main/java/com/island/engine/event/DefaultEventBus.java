package com.island.engine.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DefaultEventBus implements EventBus {
    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(Object event) {
        Class<?> eventType = event.getClass();
        subscribers.getOrDefault(eventType, List.of()).forEach(subscriber -> {
            @SuppressWarnings("unchecked")
            Consumer<Object> consumer = (Consumer<Object>) subscriber;
            consumer.accept(event);
        });
    }

    @Override
    public <E> void subscribe(Class<E> eventType, Consumer<E> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }
}
