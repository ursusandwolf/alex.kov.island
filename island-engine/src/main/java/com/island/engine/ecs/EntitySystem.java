package com.island.engine.ecs;

import com.island.engine.core.EngineAPI;
import java.util.List;
import com.island.engine.model.Mortal;
import com.island.engine.service.CellService;

/**
 * A specialized CellService that operates on entities based on their components.
 *
 * @param <T> The base type of entities.
 */
@EngineAPI
public interface EntitySystem<T extends Mortal> extends CellService<T> {
    /**
     * Gets the list of component classes that this system reads from.
     * Used for parallel conflict detection.
     */
    default List<Class<? extends Component>> readComponents() {
        return List.of();
    }
    
    /**
     * Gets the list of component classes that this system writes to.
     * Used for parallel conflict detection.
     */
    default List<Class<? extends Component>> writeComponents() {
        return List.of();
    }
}