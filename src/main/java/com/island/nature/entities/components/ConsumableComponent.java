package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import java.util.function.ToLongFunction;
import lombok.Builder;
import lombok.Getter;

/**
 * Component for entities that can be consumed (eaten) by others.
 * Helps to avoid instanceof checks in FeedingService/System.
 */
@Getter
@Builder
public class ConsumableComponent implements Component {
    private final boolean isAnimal;
    
    /**
     * Logic to handle consumption.
     * Takes requested amount, returns actual gain for the consumer.
     */
    private final ToLongFunction<Long> consumeAction;

    public long consume(long requestedAmount) {
        return consumeAction != null ? consumeAction.applyAsLong(requestedAmount) : 0;
    }
}
