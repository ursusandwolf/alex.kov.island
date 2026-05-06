package com.island.engine.ecs;

import java.util.List;
import com.island.engine.model.Mortal;

public interface EntitySystem<T extends Mortal> {
    List<Class<? extends Component>> requiredComponents();
    void process(T entity, int tickCount);
}