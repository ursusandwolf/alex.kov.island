package com.island.util.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectPoolTest {

    @Test
    @DisplayName("ObjectPool: Acquire and release")
    void pool_test() {
        ObjectPool<TestPoolable> pool = new ObjectPool<>(TestPoolable::new);
        
        TestPoolable obj1 = pool.acquire();
        assertNotNull(obj1);
        assertEquals(0, pool.size());
        
        pool.release(obj1);
        assertEquals(1, pool.size());
        assertTrue(obj1.isReset());
        
        TestPoolable obj2 = pool.acquire();
        assertSame(obj1, obj2);
        assertEquals(0, pool.size());
        
        TestPoolable obj3 = pool.acquire();
        assertNotSame(obj1, obj3);
        assertEquals(0, pool.size());
    }

    @Test
    @DisplayName("ObjectPool: Handle null release")
    void pool_null_release_test() {
        ObjectPool<TestPoolable> pool = new ObjectPool<>(TestPoolable::new);
        pool.release(null);
        assertEquals(0, pool.size());
    }

    private static class TestPoolable implements Poolable {
        private boolean reset = false;
        @Override
        public void reset() { reset = true; }
        public boolean isReset() { return reset; }
    }
}
