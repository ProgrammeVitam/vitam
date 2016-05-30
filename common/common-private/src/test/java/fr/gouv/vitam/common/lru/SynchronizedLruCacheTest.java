package fr.gouv.vitam.common.lru;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.junit.Test;

import fr.gouv.vitam.common.ResourcesPrivateUtilTest;
import fr.gouv.vitam.common.exception.VitamException;

public class SynchronizedLruCacheTest {
    private static final Integer ONE = new Integer(1);
    private static final Integer TWO = new Integer(2);
    private static final Integer THREE = new Integer(3);
    private static final Integer FOUR = new Integer(4);

    @Test
    public final void testNoInitialCapacity() throws InterruptedException {
        final SynchronizedLruCache<String, Integer> synchronizedLruCache = new SynchronizedLruCache<>(2, 100);
        assertEquals(100, synchronizedLruCache.getTtl());
        assertEquals(2, synchronizedLruCache.getCapacity());
        synchronizedLruCache.put("key", 1);
        assertEquals(ONE, synchronizedLruCache.get("key"));
        assertTrue(synchronizedLruCache.contains("key"));
        assertEquals(1, synchronizedLruCache.size());
        synchronizedLruCache.put("key2", 2);
        assertEquals(TWO, synchronizedLruCache.get("key2"));
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.put("key3", 3);
        assertEquals(THREE, synchronizedLruCache.get("key3"));
        assertEquals(2, synchronizedLruCache.size());
        Thread.sleep(200);
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.forceClearOldest();
        assertEquals(0, synchronizedLruCache.size());
        synchronizedLruCache.put("key", 1);
        assertEquals(ONE, synchronizedLruCache.get("key"));
        assertEquals(1, synchronizedLruCache.size());
        synchronizedLruCache.clear();;
        assertEquals(0, synchronizedLruCache.size());
        synchronizedLruCache.put("key", 1);
        assertEquals(ONE, synchronizedLruCache.get("key"));
        assertEquals(1, synchronizedLruCache.size());
        synchronizedLruCache.put("key2", 2);
        assertEquals(TWO, synchronizedLruCache.get("key2"));
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.remove("key");
        assertEquals(TWO, synchronizedLruCache.get("key2"));
        assertEquals(1, synchronizedLruCache.size());
        synchronizedLruCache.remove("key");
        assertEquals(TWO, synchronizedLruCache.get("key2"));
        assertEquals(1, synchronizedLruCache.size());
        try {
            assertEquals(TWO, synchronizedLruCache.get("key2",
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return FOUR;
                    }
                }));
        } catch (final VitamException e) {// NOSONAR
            fail(ResourcesPrivateUtilTest.SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        assertEquals(1, synchronizedLruCache.size());
        try {
            assertEquals(FOUR, synchronizedLruCache.get("key",
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return FOUR;
                    }
                }));
        } catch (final VitamException e) {// NOSONAR
            fail(ResourcesPrivateUtilTest.SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.setNewTtl(10);
        Thread.sleep(20);
        synchronizedLruCache.forceClearOldest();
        synchronizedLruCache.updateTtl("key");
        Thread.sleep(20);
        synchronizedLruCache.forceClearOldest();
        assertEquals(1, synchronizedLruCache.size());
        Thread.sleep(100);
        synchronizedLruCache.forceClearOldest();
        assertEquals(0, synchronizedLruCache.size());
        assertTrue(synchronizedLruCache.isEmpty());
    }

    @Test
    public final void testFullConstructor() throws InterruptedException {
        final SynchronizedLruCache<String, Integer> synchronizedLruCache = new SynchronizedLruCache<>(2, 100, 1);
        assertEquals(100, synchronizedLruCache.getTtl());
        assertEquals(2, synchronizedLruCache.getCapacity());
        synchronizedLruCache.put("key", 1);
        assertEquals(ONE, synchronizedLruCache.get("key"));
        assertEquals(1, synchronizedLruCache.size());
        synchronizedLruCache.put("key2", 2);
        assertEquals(TWO, synchronizedLruCache.get("key2"));
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.put("key3", 3);
        assertEquals(THREE, synchronizedLruCache.get("key3"));
        assertEquals(2, synchronizedLruCache.size());
        Thread.sleep(200);
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.forceClearOldest();
        assertEquals(0, synchronizedLruCache.size());
    }

    @Test
    public final void testError() {
        try {
            final SynchronizedLruCache<String, Integer> synchronizedLruCache = new SynchronizedLruCache<>(2, -100, 1);
            fail(ResourcesPrivateUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
            synchronizedLruCache.clear();
        } catch (final IllegalArgumentException e) {// NOSONAR
            // Ignore
        }

    }
}
