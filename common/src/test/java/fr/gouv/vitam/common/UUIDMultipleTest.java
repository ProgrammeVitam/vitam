package fr.gouv.vitam.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import fr.gouv.vitam.common.UUID;
import fr.gouv.vitam.common.UUID22;
import fr.gouv.vitam.common.UUIDFactory;
import fr.gouv.vitam.common.UUIDMultiple;
import fr.gouv.vitam.common.exception.InvalidUuidOperationException;

@SuppressWarnings({"javadoc", "rawtypes"})
public class UUIDMultipleTest {
    private static final int b32length = UUID22.KEYB32SIZE;

    @Test
    public void testMultipleUUID22() {
        try {
            final UUIDFactory factory = new UUIDFactory();
            final UUIDMultiple multiple = new UUIDMultiple(factory);
            final UUID id1 = factory.newUuid();
            final UUID id2 = factory.newUuid();
            final UUID id3 = factory.newUuid();
            final String ids = multiple.assembleUuids(id1, id2, id3);
            assertTrue(multiple.isMultipleUUID(ids));
            assertFalse(multiple.isMultipleUUID(id1.toString()));
            assertEquals(id1, multiple.getFirst(ids));
            assertEquals(id3, multiple.getLast(ids));
            assertEquals(id2, multiple.getUuids(ids)[1]);
            assertEquals(3, multiple.getUuidNb(ids));
            assertEquals(id1.toString(), multiple.getFirstAsString(ids));
            assertEquals(id3.toString(), multiple.getLastAsString(ids));
        } catch (final InvalidUuidOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMultipleUUID22s() {
        try {
            final UUIDFactory factory = new UUIDFactory();
            final UUIDMultiple multiple = new UUIDMultiple(factory);
            final int nb = 50000;
            final UUID22[] UUIDs = new UUID22[nb];
            final StringBuilder builder = new StringBuilder();
            final StringBuilder builder2 = new StringBuilder();
            for (int i = 0; i < nb; i++) {
                UUIDs[i] = new UUID22();
                builder.append(UUIDs[i].toString());
                builder2.append(UUIDs[i].toString());
                builder2.append(' ');
            }
            final String ids = builder.toString();
            final String ids2 = builder2.toString();
            assertEquals(b32length * nb, ids.length());
            final long start = System.currentTimeMillis();
            final UUID[] UUIDs2 = multiple.getUuids(ids);
            final long stop = System.currentTimeMillis();
            assertEquals(nb, UUIDs2.length);
            assertEquals(nb, multiple.getUuidNb(ids));
            for (int i = 0; i < nb; i++) {
                assertTrue(UUIDs[i].equals(UUIDs2[i]));
            }
            assertTrue(UUIDs[0].equals(multiple.getFirst(ids)));
            assertTrue(UUIDs[nb - 1].equals(multiple.getLast(ids)));

            assertEquals((b32length+1) * nb, ids2.length());
            final long start2 = System.currentTimeMillis();
            final UUID[] UUIDs3 = multiple.getUuidsSharp(ids2);
            final long stop2 = System.currentTimeMillis();
            assertEquals(nb, UUIDs2.length);
            for (int i = 0; i < nb; i++) {
                assertTrue(UUIDs[i].equals(UUIDs3[i]));
            }
            assertTrue(UUIDs[0].equals(multiple.getFirst(ids2)));
            System.out.println("Create " + nb + " UUIDs from 1 String in " + (stop - start) + ":" + (stop2 - start2));
        } catch (final InvalidUuidOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}