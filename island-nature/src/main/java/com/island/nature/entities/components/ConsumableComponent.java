package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import com.island.nature.model.Cell;
import lombok.Builder;
import lombok.Getter;

/**
 * Component for entities that can be consumed (eaten) by others.
 */
@Getter
@Builder
public class ConsumableComponent implements Component {
    private final boolean isAnimal;
    
    /**
     * Logic to handle consumption.
     */
    private final ConsumeAction<Cell> consumeAction;

    public long consume(long requestedAmount) {
        return consume(requestedAmount, null);
    }

    public long consume(long requestedAmount, Cell context) {
        return consumeAction != null ? consumeAction.consume(requestedAmount, context) : 0;
    }
}
