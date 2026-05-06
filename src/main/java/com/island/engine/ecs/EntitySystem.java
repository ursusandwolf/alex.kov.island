package com.island.engine.ecs;

import java.util.List;
import com.island.engine.model.Mortal;
import com.island.engine.service.CellService;

public interface EntitySystem<T extends Mortal> extends CellService<T> {
    List<Class<? extends Component>> requiredComponents();

    void process(T entity, int tickCount);
}