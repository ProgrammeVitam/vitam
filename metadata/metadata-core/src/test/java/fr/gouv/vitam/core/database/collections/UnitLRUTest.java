/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;

public class UnitLRUTest {
    private static Unit testUnit = new Unit("{\"_id\": \"" + GUIDFactory.newUnitGUID(0) + "\"}");

    // This test will be deleted because the cache will not be used any more
    @Ignore
    @Test
    public void givenMongoDbAccessConstructorWhenPutAndRemoveKeyThenWorkCorrectly() {
        final UnitLRU unitLRU = new UnitLRU();
        assertEquals(true, unitLRU.isEmpty());
        assertTrue(unitLRU.keySet().isEmpty());
        assertEquals(Collections.emptySet(), unitLRU.entrySet());
        assertEquals(true, unitLRU.values().isEmpty());

        unitLRU.put("test", testUnit);
        assertEquals(testUnit, unitLRU.get("test"));

        final Map<String, Unit> map = new HashMap<>();
        map.put("unit2", testUnit);
        map.put("unit3", testUnit);
        unitLRU.putAll(map);
        assertEquals(testUnit, unitLRU.get("unit2"));
        assertEquals(testUnit, unitLRU.get("unit3"));
        assertEquals(3, unitLRU.size());

        assertEquals(true, unitLRU.containsKey("unit2"));
        unitLRU.remove("unit2");
        assertEquals(2, unitLRU.size());
        assertEquals(false, unitLRU.containsKey("unit2"));

        assertEquals(true, unitLRU.containsValue(""));
        unitLRU.forceClearOldest();
        assertEquals(2, unitLRU.size());

        unitLRU.clear();
        assertEquals(0, unitLRU.size());

    }

}
