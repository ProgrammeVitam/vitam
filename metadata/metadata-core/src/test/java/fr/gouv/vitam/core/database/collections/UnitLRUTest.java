package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;

public class UnitLRUTest {
    private static Unit testUnit = new Unit("{\"_id\": \"" + GUIDFactory.newUnitGUID(0) + "\"}");

    @Test
    public void givenMongoDbAccessConstructorWhenPutAndRemoveKeyThenWorkCorrectly() {
        final UnitLRU unitLRU = new UnitLRU();
        assertEquals(true, unitLRU.isEmpty());
        assertEquals(null, unitLRU.keySet());
        assertEquals(Collections.emptySet(), unitLRU.entrySet());
        assertEquals(null, unitLRU.values());

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
