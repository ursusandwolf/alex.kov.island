package com.island.engine.ecs;

import java.util.List;
import com.island.engine.model.Mortal;
import com.island.nature.entities.core.Organism;

/**
 * Filter for entities based on their required components.
 */
public class EntityQuery<T extends Mortal> {
    private final List<Class<? extends Component>> requiredComponents;

    public EntityQuery(List<Class<? extends Component>> requiredComponents) {
        this.requiredComponents = List.copyOf(requiredComponents);
    }

    public boolean matches(T entity) {
        if (entity instanceof Organism organism) {
            for (Class<? extends Component> componentClass : requiredComponents) {
                if (!organism.getComponentStore().has(componentClass)) {
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
