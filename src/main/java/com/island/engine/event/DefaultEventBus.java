package com.island.engine.event;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultEventBus implements EventBus {
    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<Class<?>>> typeHierarchyCache = new ConcurrentHashMap<>();

    @Override
    public void publish(Object event) {
        Class<?> eventType = event.getClass();
        Set<Class<?>> types = typeHierarchyCache.computeIfAbsent(eventType, this::getTypeHierarchy);

        for (Class<?> type : types) {
            List<Consumer<?>> eventSubscribers = subscribers.get(type);
            if (eventSubscribers != null) {
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
        }
    }

    @Override
    public <E> void subscribe(Class<E> eventType, Consumer<E> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    @Override
    public <E> void unsubscribe(Class<E> eventType, Consumer<E> subscriber) {
        List<Consumer<?>> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers != null) {
            eventSubscribers.remove(subscriber);
        }
    }

    private Set<Class<?>> getTypeHierarchy(Class<?> type) {
        Set<Class<?>> hierarchy = new HashSet<>();
        java.util.Queue<Class<?>> queue = new java.util.LinkedList<>();
        queue.add(type);
        
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (current == null || !hierarchy.add(current)) {
                continue;
            }
            
            Class<?> superclass = current.getSuperclass();
            if (superclass != null) {
                queue.add(superclass);
            }
            
            for (Class<?> iface : current.getInterfaces()) {
                queue.add(iface);
            }
        }
        return hierarchy;
    }
}
