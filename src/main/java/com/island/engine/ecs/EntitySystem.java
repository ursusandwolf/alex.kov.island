package com.island.engine.ecs;

import java.util.List;
import com.island.engine.model.Mortal;
import com.island.engine.service.CellService;

public interface EntitySystem<T extends Mortal> extends CellService<T> {
    default List<Class<? extends Component>> readComponents() {
        return List.of();
    }
    
    default List<Class<? extends Component>> writeComponents() {
        return List.of();
    }
}