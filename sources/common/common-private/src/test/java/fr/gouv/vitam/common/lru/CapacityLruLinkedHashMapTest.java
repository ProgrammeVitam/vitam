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

import java.util.AbstractMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class CapacityLruLinkedHashMapTest {

    @Test
    public void testCapacityMustBePositive() {
        try {
            new CapacityLruLinkedHashMap<>(-627, 1, 1);
            fail(ResourcesPrivateUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            new CapacityLruLinkedHashMap<>(0, 0, 1306.9207F);
            fail(ResourcesPrivateUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
    }

    @Test
    public void testIllegalLoadFactor() {
        try {
            new CapacityLruLinkedHashMap<>(1, 0, 0);
            fail(ResourcesPrivateUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
    }

    @Test
    public void testCapacity() {
        final CapacityLruLinkedHashMap<Object, String> capacityLruLinkedHashMap0 = new CapacityLruLinkedHashMap<>(
            1,
            1,
            1
        );
        capacityLruLinkedHashMap0.put("", "");
        capacityLruLinkedHashMap0.put("capacity must be positive", "B[3aky(lHPeu\"");
        assertFalse(capacityLruLinkedHashMap0.isEmpty());
        assertEquals(1, capacityLruLinkedHashMap0.size());

        final CapacityLruLinkedHashMap<Object, Object> capacityLruLinkedHashMap1 = new CapacityLruLinkedHashMap<>(
            1,
            0,
            1.0F
        );
        final int int0 = capacityLruLinkedHashMap1.getCapacity();
        assertEquals(1, int0);
    }

    @Test
    public void testNotRemove() {
        final CapacityLruLinkedHashMap<Object, String> capacityLruLinkedHashMap0 = new CapacityLruLinkedHashMap<>(
            3384,
            0,
            3384
        );
        final AbstractMap.SimpleEntry<Object, String> abstractMap_SimpleEntry0 = new AbstractMap.SimpleEntry<>(
            (Object) null,
            "capacity must be positive"
        );
        final boolean boolean0 = capacityLruLinkedHashMap0.removeEldestEntry(abstractMap_SimpleEntry0);
        assertFalse(boolean0);
    }
}
