package com.island.engine.ecs;

import java.util.BitSet;
import java.util.List;
import com.island.engine.model.Mortal;

/**
 * Filter for entities based on their required components.
 */
public class EntityQuery<T extends Mortal> {
    private final List<Class<? extends Component>> requiredComponents;
    private BitSet requiredBitSet;

    public EntityQuery(List<Class<? extends Component>> requiredComponents) {
        this.requiredComponents = List.copyOf(requiredComponents);
    }

    public void bind(ComponentRegistry registry) {
        this.requiredBitSet = registry.getBitSet(requiredComponents);
    }

    public boolean matches(T entity) {
        if (entity instanceof Entity e) {
            EntityArchetype archetype = e.getArchetype();
            if (archetype != null && requiredBitSet != null) {
                return matches(archetype);
            }
            // Fallback for non-archetype entities or unbound queries
            for (Class<? extends Component> componentClass : requiredComponents) {
                if (!e.getComponentStore().has(componentClass)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean matches(EntityArchetype archetype) {
        if (requiredBitSet == null) {
            return true; // Or throw, but true is safer if unbound
        }
        return archetype.containsAll(requiredBitSet);
    }

    public List<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }
}
