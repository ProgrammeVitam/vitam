/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.lru;

import fr.gouv.vitam.common.ResourcesPrivateUtilTest;
import fr.gouv.vitam.common.exception.VitamException;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        synchronizedLruCache.clear();
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
            assertEquals(
                TWO,
                synchronizedLruCache.get(
                    "key2",
                    new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return FOUR;
                        }
                    }
                )
            );
        } catch (final VitamException e) { // NOSONAR
            fail(ResourcesPrivateUtilTest.SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        assertEquals(1, synchronizedLruCache.size());
        try {
            assertEquals(
                FOUR,
                synchronizedLruCache.get(
                    "key",
                    new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return FOUR;
                        }
                    }
                )
            );
        } catch (final VitamException e) { // NOSONAR
            fail(ResourcesPrivateUtilTest.SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.setNewTtl(10);
        Thread.sleep(20);
        synchronizedLruCache.forceClearOldest();
        synchronizedLruCache.updateTtl("key");
        synchronizedLruCache.forceClearOldest();
        Thread.sleep(100);
        synchronizedLruCache.forceClearOldest();
        assertEquals(0, synchronizedLruCache.size());
        assertTrue(synchronizedLruCache.isEmpty());
    }

    @Test
    public final void testFullConstructor() throws InterruptedException {
        final SynchronizedLruCache<String, Integer> synchronizedLruCache = new SynchronizedLruCache<>(2, 200, 1);
        assertEquals(200, synchronizedLruCache.getTtl());
        assertEquals(2, synchronizedLruCache.getCapacity());
        synchronizedLruCache.put("key001", 1);
        assertEquals(ONE, synchronizedLruCache.get("key001"));
        assertEquals(1, synchronizedLruCache.size());
        synchronizedLruCache.put("key002", 2);
        assertEquals(TWO, synchronizedLruCache.get("key002"));
        assertEquals(2, synchronizedLruCache.size());
        synchronizedLruCache.put("key003", 3);
        assertEquals(THREE, synchronizedLruCache.get("key003"));
        assertEquals(2, synchronizedLruCache.size());
        Thread.sleep(400);
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
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
    }
}
