/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.guid;

import fr.gouv.vitam.common.ResourcesPublicUtilTest;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "javadoc" })
public class GUIDImplPublicTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GUIDImplPublicTest.class);

    private static final int VERSION = 1 & 0x1F;
    private static int NB = 100000;
    private static final int HEX_LENGTH = GUIDImplPrivate.KEYSIZE * 2;

    @Test
    public void testStructure() {
        final GUIDImplPrivate id = new GUIDImplPrivate();
        final String str = id.toHex();

        assertEquals('0', str.charAt(0));
        assertEquals('1', str.charAt(1));
        assertEquals(HEX_LENGTH, str.length());
        LOGGER.info(id.toArk() + " = " + id.toString());
    }

    @Test
    public void testParsing() {
        for (int i = 0; i < 1000; i++) {
            final GUIDImplPrivate id1 = new GUIDImplPrivate();
            GUIDImplPrivate id2;
            try {
                id2 = new GUIDImplPrivate(id1.toHex());
                assertEquals(id1, id2);
                assertEquals(id1.hashCode(), id2.hashCode());
                assertEquals(0, id1.compareTo(id2));

                final GUIDImplPrivate id3 = new GUIDImplPrivate(id1.getBytes());
                assertEquals(id1, id3);
                assertEquals(id1.hashCode(), id3.hashCode());
                assertEquals(0, id1.compareTo(id3));

                final GUIDImplPrivate id4 = new GUIDImplPrivate(id1.toBase32());
                assertEquals(id1, id4);
                assertEquals(id1.hashCode(), id4.hashCode());
                assertEquals(0, id1.compareTo(id4));

                final GUIDImplPrivate id5 = new GUIDImplPrivate(id1.toArk());
                assertEquals(id1, id5);
                assertEquals(id1.hashCode(), id5.hashCode());
                assertEquals(0, id1.compareTo(id5));
            } catch (final InvalidGuidOperationException e) {
                LOGGER.error(e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testNonSequentialValue() {
        final int n = NB / 2;
        final String[] ids = new String[n];

        final long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new GUIDImplPrivate().toBase32();
        }
        final long stop = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        final long start2 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new GUIDImplPrivate().toHex();
        }
        final long stop2 = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        final long start4 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new GUIDImplPrivate().toBase32();
        }
        final long stop4 = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        final long start5 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new GUIDImplPrivate().toHex();
        }
        final long stop5 = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        LOGGER.info(
            "B32: " +
            (stop - start) +
            " vsHex: " +
            (stop2 - start2) +
            " vsB32: " +
            (stop4 - start4) +
            " vxHex: " +
            (stop5 - start5)
        );
    }

    @Test
    public void testGetBytesImmutability() {
        final GUIDImplPrivate id = new GUIDImplPrivate();
        final byte[] bytes = id.getBytes();
        final byte[] original = Arrays.copyOf(bytes, bytes.length);
        bytes[0] = 0;
        bytes[1] = 0;
        bytes[2] = 0;

        assertTrue(Arrays.equals(id.getBytes(), original));
    }

    @Test
    public void testConstructorImmutability() {
        final GUIDImplPrivate id = new GUIDImplPrivate();
        final byte[] bytes = id.getBytes();
        final byte[] original = Arrays.copyOf(bytes, bytes.length);

        try {
            final GUIDImplPrivate id2 = new GUIDImplPrivate(bytes);
            bytes[0] = 0;
            bytes[1] = 0;

            assertTrue(Arrays.equals(id2.getBytes(), original));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testVersionField() {
        final GUIDImplPrivate generated = new GUIDImplPrivate();
        assertEquals(VERSION, generated.getVersion());

        try {
            final GUIDImplPrivate parsed1 = new GUIDImplPrivate("aeaqaaaaaitxll67abarqaktftcfyniaaaaq");
            assertEquals(VERSION, parsed1.getVersion());
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testHexBase32() {
        try {
            final GUIDImplPrivate parsed1 = new GUIDImplPrivate("aeaqaaaaaitxll67abarqaktftcfyniaaaaq");
            final GUIDImplPrivate parsed2 = new GUIDImplPrivate("0101000000022775afdf00411801532cc45c35000001");
            final GUIDImplPrivate parsed0 = new GUIDImplPrivate("AQEAAAACJ3Wv3wBBGAFTLMRcNQAAAQ");
            assertTrue(parsed1.equals(parsed2));
            assertTrue(parsed1.equals(parsed0));
            final GUIDImplPrivate generated = new GUIDImplPrivate();
            final GUIDImplPrivate parsed3 = new GUIDImplPrivate(generated.getBytes());
            final GUIDImplPrivate parsed4 = new GUIDImplPrivate(generated.toBase32());
            final GUIDImplPrivate parsed5 = new GUIDImplPrivate(generated.toHex());
            final GUIDImplPrivate parsed6 = new GUIDImplPrivate(generated.toString());
            final GUIDImplPrivate parsed7 = new GUIDImplPrivate(generated.toBase64());
            assertTrue(generated.equals(parsed3));
            assertTrue(generated.equals(parsed4));
            assertTrue(generated.equals(parsed5));
            assertTrue(generated.equals(parsed6));
            assertTrue(generated.equals(parsed7));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPIDField() {
        final GUIDImplPrivate id = new GUIDImplPrivate();
        assertEquals(GUIDImplPrivate.jvmProcessId(), id.getProcessId());
    }

    @Test
    public void testDateField() {
        final GUIDImplPrivate id = new GUIDImplPrivate();
        assertTrue(id.getTimestamp() > new Date().getTime() - 100);
        assertTrue(id.getTimestamp() < new Date().getTime() + 100);
    }

    @Test
    public void testObjectIdField() {
        GUIDImplPrivate id = new GUIDImplPrivate();
        assertEquals(0, id.getObjectId());
        for (int i = 0; i < 32; i++) {
            id = new GUIDImplPrivate(i, 0);
            assertEquals(i, id.getObjectId());
        }
    }

    @Test
    public void testDomainIdField() {
        GUIDImplPrivate id = new GUIDImplPrivate();
        assertEquals(0, id.getTenantId());
        for (int i = 0; i < 65535; i++) {
            id = new GUIDImplPrivate(0, i);
            assertEquals(i, id.getTenantId());
        }
        for (int i = 0; i < 255; i++) {
            id = new GUIDImplPrivate(0, 0x3FFFFFFF - i);
            assertEquals(0x3FFFFFFF - i, id.getTenantId());
        }
    }

    @Test
    public void testPlatformIdField() {
        GUIDImplPrivate id = new GUIDImplPrivate();
        assertEquals(ServerIdentity.getInstance().getGlobalPlatformId(), id.getPlatformId());
        for (int i = 0; i < 65535; i++) {
            id = new GUIDImplPrivate(0, 0, i);
            assertEquals(i, id.getPlatformId());
            assertFalse(id.isWorm());
        }
        for (int i = 0; i < 255; i++) {
            id = new GUIDImplPrivate(0, 0, 0x7FFFFFFF - i);
            assertEquals(0x7FFFFFFF - i, id.getPlatformId());
            assertFalse(id.isWorm());
        }
        // WORM
        for (int i = 0; i < 65535; i++) {
            id = new GUIDImplPrivate(0, 0, i, true);
            assertEquals(i, id.getPlatformId());
            assertTrue(id.isWorm());
        }
        for (int i = 0; i < 255; i++) {
            id = new GUIDImplPrivate(0, 0, 0x7FFFFFFF - i, true);
            assertEquals(0x7FFFFFFF - i, id.getPlatformId());
            assertTrue(id.isWorm());
        }
    }

    @Test
    public void testForDuplicates() {
        final int n = NB;
        final Set<GUIDImplPrivate> UUIDs = new HashSet<>();
        final GUIDImplPrivate[] UUIDArray = new GUIDImplPrivate[n];

        final long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            UUIDArray[i] = new GUIDImplPrivate();
        }
        final long stop = System.currentTimeMillis();
        LOGGER.info("TimeSequential = " + (stop - start) + " so " + (n * 1000) / (stop - start) + " UUID22s/s");

        for (int i = 0; i < n; i++) {
            UUIDs.add(UUIDArray[i]);
        }

        LOGGER.info("Create " + n + " and get: " + UUIDs.size());
        assertEquals(n, UUIDs.size());
        checkConsecutive(UUIDArray);
    }

    @Test
    public void checkShuffleInteger() {
        final byte[] uuid = new byte[4];
        for (int src = 0; src < NB; src++) {
            uuid[1] = (byte) (src & 0xFF);
            uuid[2] = (byte) ((src >> 8) & 0xFF);
            uuid[3] = (byte) ((src >> 16) & 0xFF);
            int count = (uuid[3] & 0xFF) << 16;
            count |= (uuid[2] & 0xFF) << 8;
            count |= uuid[1] & 0xFF;
            assertEquals(src, count);
        }
    }

    private void checkConsecutive(final GUIDImplPrivate[] UUIDArray) {
        final int n = UUIDArray.length;
        int i = 1;
        int largest = 0;
        for (; i < n; i++) {
            if (UUIDArray[i].getTimestamp() > UUIDArray[i - 1].getTimestamp()) {
                int j = i + 1;
                final long time = UUIDArray[i].getTimestamp();
                for (; j < n; j++) {
                    assertEquals(-1, UUIDArray[i].compareTo(UUIDArray[j]));
                    if (UUIDArray[j].getTimestamp() > time) {
                        if (largest < j - i) {
                            largest = j - i;
                        }
                        i = j;
                        break;
                    }
                }
            } else {
                assertEquals(-1, UUIDArray[i - 1].compareTo(UUIDArray[i]));
                int j = i + 1;
                final long time = UUIDArray[i].getTimestamp();
                for (; j < n; j++) {
                    assertEquals(-1, UUIDArray[i - 1].compareTo(UUIDArray[j]));
                    if (UUIDArray[j].getTimestamp() > time) {
                        if (largest < j - i + 1) {
                            largest = j - i + 1;
                        }
                        i = j;
                        break;
                    }
                }
            }
        }
        LOGGER.info(largest + " different consecutive elements");
    }

    private static class Generator extends Thread {

        private final GUIDImplPrivate[] UUIDs;
        int base;
        int n;

        public Generator(final int n, final GUIDImplPrivate[] UUIDs, final int base) {
            this.n = n;
            this.UUIDs = UUIDs;
            this.base = base;
        }

        @Override
        public void run() {
            for (int i = 0; i < n; i++) {
                UUIDs[base + i] = new GUIDImplPrivate();
            }
        }
    }

    @Test
    public void testConcurrentGeneration() throws InterruptedException {
        final int numThreads = Runtime.getRuntime().availableProcessors() + 1;
        final Thread[] threads = new Thread[numThreads];
        final int n = NB;
        final int step = n / numThreads;
        final GUIDImplPrivate[] UUIDs = new GUIDImplPrivate[step * numThreads];

        final long start = System.currentTimeMillis();
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Generator(step, UUIDs, i * step);
            threads[i].start();
        }

        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        final long stop = System.currentTimeMillis();

        final Set<GUIDImplPrivate> UUIDSet = new HashSet<>();

        for (final GUIDImplPrivate uuid : UUIDs) {
            UUIDSet.add(uuid);
        }

        assertEquals(UUIDs.length, UUIDSet.size());
        UUIDSet.clear();
        LOGGER.info(
            "TimeConcurrent = " + (stop - start) + " so " + (UUIDs.length * 1000) / (stop - start) + " UUID22s/s"
        );
        final TreeSet<GUIDImplPrivate> set = new TreeSet<>();
        for (final GUIDImplPrivate uuid : UUIDs) {
            set.add(uuid);
        }
        checkConsecutive(set.toArray(new GUIDImplPrivate[0]));
    }

    @Test
    public void testJsonXml() {
        final GUIDImplPrivate uuid = new GUIDImplPrivate(1, 2);
        LOGGER.info("HEX:" + uuid.toHex());
        LOGGER.info("BASE32: " + uuid.toBase32());
        LOGGER.info("BASE64: " + uuid.toBase64());
        try {
            final String json = JsonHandler.writeAsString(uuid);
            LOGGER.info(json);
            final GUIDImplPrivate uuid2 = JsonHandler.getFromString(json, GUIDImplPrivate.class);
            assertEquals("Json check", uuid, uuid2);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            fail("Exception occurs: " + e.getMessage());
            return;
        }
    }

    @Test
    public void testConstruct() {
        assertTrue(new GUIDImplPrivate(1).getObjectId() == 1);
        assertTrue(new GUIDImplPrivate(true).isWorm());
        assertTrue(new GUIDImplPrivate(1, false).getObjectId() == 1);
        assertTrue(new GUIDImplPrivate(1, 2, false).getTenantId() == 2);
        assertTrue(new GUIDImplPrivate(1, 2, 3, false).getPlatformId() == 3);
        try {
            new GUIDImplPrivate(-1, 2, 3, false);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            new GUIDImplPrivate(0x1FF, 2, 3, false);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            new GUIDImplPrivate(1, -2, 3, false);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            new GUIDImplPrivate(1, 0x4FFFFFFF, 3, false);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            new GUIDImplPrivate(1, 2, -3, false);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            new GUIDImplPrivate(1, 2, 0x80000000, false);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        final GUID guid = GUIDFactory.newEventGUID(2);
        assertNotNull(guid.toString());
        assertNotNull(guid.toHex());
        assertNotNull(guid.toBase32());
        assertNotNull(guid.toBase64());
        assertNotNull(guid.toArk());
        assertTrue(guid.getCounter() >= 0);
        assertTrue(guid.getObjectId() == GUIDObjectType.EVENT_TYPE);
        assertTrue(guid.getPlatformId() >= 0);
        assertTrue(guid.getProcessId() >= 0);
        assertTrue(guid.getTenantId() == 2);
        assertTrue(guid.getTimestamp() > 0);
        assertTrue(guid.getVersion() == GUIDImpl.VERSION);
        assertTrue(guid.hashCode() != 0);
        assertNotNull(guid.toArkName());
        assertNotNull(guid.getMacFragment());
        assertNotNull(guid.getBytes());
    }
}
