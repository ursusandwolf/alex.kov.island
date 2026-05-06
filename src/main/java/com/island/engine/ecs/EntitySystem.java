package com.island.engine.ecs;

import com.island.engine.Mortal;
import java.util.List;

public interface EntitySystem<T extends Mortal> {
    List<Class<? extends Component>> requiredComponents();
    void process(T entity, int tickCount);
}
