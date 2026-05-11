package com.island.engine.ecs;

import com.island.engine.core.EngineAPI;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central registry for ECS component mapping.
 * 
 * <p>Maps component classes to stable integer indices. These indices are used 
 * by high-performance storage implementations (like {@link ArrayComponentStore}) 
 * for constant-time access.</p>
 * 
 * <p>This registry also manages the {@link EntityArchetype} cache, ensuring that 
 * entities with identical component signatures share the same archetype object.</p>
 * 
 * @since 1.0
 */
@EngineAPI
public final class ComponentRegistry {
    private final Map<Class<? extends Component>, Integer> indices = new ConcurrentHashMap<>();
    private final Map<BitSet, EntityArchetype> archetypeCache = new ConcurrentHashMap<>();
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    /**
     * Retrieves the unique index for a component type, registering it if necessary.
     * 
     * @param type the component class.
     * @return a unique, stable integer index.
     */
    public int getOrRegister(Class<? extends Component> type) {
        return indices.computeIfAbsent(type, k -> nextIndex.getAndIncrement());
    }

    /**
     * Retrieves or creates an archetype for the given set of component indices.
     * 
     * @param bitSet the signature representing a combination of components.
     * @return a cached or new EntityArchetype.
     */
    public EntityArchetype getArchetype(BitSet bitSet) {
        return archetypeCache.computeIfAbsent(bitSet, EntityArchetype::new);
    }

    /**
     * Converts a collection of component types into a BitSet signature.
     * 
     * @param types the component classes.
     * @return a BitSet where bits corresponding to component indices are set.
     */
    public BitSet getBitSet(Collection<Class<? extends Component>> types) {
        BitSet bitSet = new BitSet();
        for (Class<? extends Component> type : types) {
            bitSet.set(getOrRegister(type));
        }
        return bitSet;
    }

    /**
     * Returns the total number of unique component types registered.
     */
    public int size() {
        return nextIndex.get();
    }
}
