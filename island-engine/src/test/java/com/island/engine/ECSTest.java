package com.island.engine;

import com.island.engine.ecs.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ECSTest {

    @Test
    @DisplayName("ComponentRegistry: Register and retrieve indices")
    void component_registry_test() {
        ComponentRegistry registry = new ComponentRegistry();
        int idx1 = registry.getOrRegister(TestComponent.class);
        int idx2 = registry.getOrRegister(OtherComponent.class);
        
        assertEquals(0, idx1);
        assertEquals(1, idx2);
        assertEquals(idx1, registry.getOrRegister(TestComponent.class));
    }

    @Test
    @DisplayName("ComponentStore: Add and get components")
    void component_store_test() {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentStore store = ComponentStore.createDefault(registry);
        
        TestComponent comp = new TestComponent();
        store.add(comp);
        
        assertTrue(store.has(TestComponent.class));
        assertEquals(comp, store.get(TestComponent.class));
        assertFalse(store.has(OtherComponent.class));
        
        BitSet bitSet = store.getComponentBitSet();
        assertTrue(bitSet.get(registry.getOrRegister(TestComponent.class)));
    }

    @Test
    @DisplayName("EntityQuery: Filtering logic")
    void entity_query_test() {
        ComponentRegistry registry = new ComponentRegistry();
        EntityQuery<Entity> query = new EntityQuery<>(List.of(TestComponent.class));
        query.bind(registry);
        
        BitSet bs1 = new BitSet();
        bs1.set(registry.getOrRegister(TestComponent.class));
        EntityArchetype arch1 = registry.getArchetype(bs1);
        
        BitSet bs2 = new BitSet();
        bs2.set(registry.getOrRegister(OtherComponent.class));
        EntityArchetype arch2 = registry.getArchetype(bs2);
        
        assertTrue(query.matches(arch1));
        assertFalse(query.matches(arch2));
    }

    private static class TestComponent implements Component {}
    private static class OtherComponent implements Component {}
}
