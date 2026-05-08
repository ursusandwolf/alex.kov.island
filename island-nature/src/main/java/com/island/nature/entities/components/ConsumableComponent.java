package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import lombok.Builder;
import lombok.Getter;

/**
 * Component for entities that can be consumed (eaten) by others.
 *
 * @param <T> The type of context required for consumption.
 */
@Getter
@Builder
public class ConsumableComponent<T> implements Component {
    private final boolean isAnimal;
    
    /**
     * Logic to handle consumption.
     */
    private final ConsumeAction<T> consumeAction;

    public long consume(long requestedAmount) {
        return consume(requestedAmount, null);
    }

    public long consume(long requestedAmount, T context) {
        return consumeAction != null ? consumeAction.consume(requestedAmount, context) : 0;
    }
}
