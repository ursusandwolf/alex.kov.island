package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import java.util.function.BiFunction;
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
     * Takes requested amount and context (e.g. Cell), returns actual gain for the consumer.
     */
    private final BiFunction<Long, Object, Long> consumeAction;

    public long consume(long requestedAmount) {
        return consume(requestedAmount, null);
    }

    public long consume(long requestedAmount, Object context) {
        return consumeAction != null ? consumeAction.apply(requestedAmount, context) : 0;
    }
}
