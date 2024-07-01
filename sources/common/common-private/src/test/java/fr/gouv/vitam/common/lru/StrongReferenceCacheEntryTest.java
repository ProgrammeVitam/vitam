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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StrongReferenceCacheEntryTest {

    @Test
    public void testTtlMustBePositive() {
        final Integer integer0 = new Integer(-1324);
        try {
            new StrongReferenceCacheEntry<>(integer0, -1324);
            fail(ResourcesPrivateUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            new StrongReferenceCacheEntry<>("AU4LKOz]pz+", 0L);
            fail(ResourcesPrivateUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
    }

    @Test
    public void testStillValid() {
        final Integer integer0 = new Integer(227);
        final StrongReferenceCacheEntry<Integer> strongReferenceCacheEntry0 = new StrongReferenceCacheEntry<>(
            integer0,
            1L
        );
        strongReferenceCacheEntry0.isStillValid(189L);
        final boolean boolean0 = strongReferenceCacheEntry0.isStillValid(1464473409004L);
        assertTrue(boolean0);
    }

    @Test
    public void testNotStillValid() {
        final StrongReferenceCacheEntry<String> strongReferenceCacheEntry0 = new StrongReferenceCacheEntry<>(
            "ttlmst be positive",
            1987L
        );
        strongReferenceCacheEntry0.resetTime(-1324);
        final String string0 = strongReferenceCacheEntry0.getValue();
        assertNull(string0);
    }

    @Test
    public void testValue() {
        final StrongReferenceCacheEntry<String> strongReferenceCacheEntry0 = new StrongReferenceCacheEntry<>("", 71L);
        final String string0 = strongReferenceCacheEntry0.getValue();
        assertEquals("", string0);
    }
}
