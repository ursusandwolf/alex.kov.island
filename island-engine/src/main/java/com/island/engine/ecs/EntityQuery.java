package com.island.engine.ecs;

import com.island.engine.core.EngineAPI;
import java.util.BitSet;
import java.util.List;
import com.island.engine.model.Mortal;

/**
 * A filter used to select entities that possess a specific set of components.
 * 
 * <p>Queries should be created once and then "bound" to a {@link ComponentRegistry} 
 * to enable high-performance matching via bitset signatures.</p>
 * 
 * <pre>{@code
 * EntityQuery<Organism> predatorQuery = new EntityQuery<>(List.of(
 *     PositionComponent.class, 
 *     HungerComponent.class
 * ));
 * predatorQuery.bind(registry);
 * 
 * world.query(predatorQuery, entity -> {
 *     // This block runs only for entities with both components
 * });
 * }</pre>
 *
 * @param <T> The base type of entities.
 * @since 1.0
 */
@EngineAPI
public class EntityQuery<T extends Mortal> {
    private final List<Class<? extends Component>> requiredComponents;
    private BitSet requiredBitSet;

    /**
     * Creates a new query with the specified requirements.
     * 
     * @param requiredComponents the list of component classes that an entity must have.
     */
    public EntityQuery(List<Class<? extends Component>> requiredComponents) {
        this.requiredComponents = List.copyOf(requiredComponents);
    }

    /**
     * Binds this query to a registry, calculating internal bitset signatures 
     * for optimized matching.
     * 
     * @param registry the component registry to use for index mapping.
     */
    public void bind(ComponentRegistry registry) {
        this.requiredBitSet = registry.getBitSet(requiredComponents);
    }

    /**
     * Checks if the given entity matches the query criteria.
     * 
     * @param entity the entity to check.
     * @return {@code true} if the entity possesses all required components.
     */
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

    /**
     * Checks if the given archetype matches the query criteria.
     * 
     * @param archetype the entity archetype to check.
     * @return {@code true} if the archetype signature contains all required components.
     */
    public boolean matches(EntityArchetype archetype) {
        if (requiredBitSet == null) {
            return true; // Or throw, but true is safer if unbound
        }
        return archetype.containsAll(requiredBitSet);
    }

    /**
     * Returns the list of component types required by this query.
     */
    public List<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }
}
