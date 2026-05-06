package com.island.engine.ecs;

import java.util.List;
import com.island.engine.model.Mortal;

/**
 * Filter for entities based on their required components.
 */
public class EntityQuery<T extends Mortal> {
    private final List<Class<? extends Component>> requiredComponents;

    public EntityQuery(List<Class<? extends Component>> requiredComponents) {
        this.requiredComponents = List.copyOf(requiredComponents);
    }

    public boolean matches(T entity) {
        if (entity instanceof Entity e) {
            for (Class<? extends Component> componentClass : requiredComponents) {
                if (!e.getComponentStore().has(componentClass)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public List<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }
}
