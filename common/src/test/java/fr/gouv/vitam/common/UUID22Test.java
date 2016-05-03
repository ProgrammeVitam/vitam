package fr.gouv.vitam.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import fr.gouv.vitam.common.UUID22;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.InvalidUuidOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.XmlHandler;

@SuppressWarnings({"javadoc"})
public class UUID22Test {
    private static final int VERSION = (1 & 0x1F);
    private static int NB = 500000;
    private static final int hexlength = UUID22.KEYSIZE * 2;

    @Test
    public void testStructure() {
        final UUID22 id = new UUID22();
        final String str = id.toHex();

        assertEquals('0', str.charAt(0));
        assertEquals('1', str.charAt(1));
        assertEquals(hexlength, str.length());
        System.out.println(id.toArk()+" "+id.toString());
    }

    @Test
    public void testParsing() {
        for (int i = 0; i < NB; i++) {
            final UUID22 id1 = new UUID22();
            UUID22 id2;
            try {
                id2 = new UUID22(id1.toHex());
                assertEquals(id1, id2);
                assertEquals(id1.hashCode(), id2.hashCode());
                assertEquals(0, id1.compareTo(id2));

                final UUID22 id3 = new UUID22(id1.getBytes());
                assertEquals(id1, id3);
                assertEquals(id1.hashCode(), id3.hashCode());
                assertEquals(0, id1.compareTo(id3));

                final UUID22 id4 = new UUID22(id1.toBase32());
                assertEquals(id1, id4);
                assertEquals(id1.hashCode(), id4.hashCode());
                assertEquals(0, id1.compareTo(id4));
                
                final UUID22 id5 = new UUID22(id1.toArk());
                assertEquals(id1, id5);
                assertEquals(id1.hashCode(), id5.hashCode());
                assertEquals(0, id1.compareTo(id5));
            } catch (final InvalidUuidOperationException e) {
                e.printStackTrace();
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
            ids[i] = new UUID22().toBase32();
        }
        final long stop = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        final long start2 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new UUID22().toHex();
        }
        final long stop2 = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        final long start4 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new UUID22().toBase32();
        }
        final long stop4 = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        final long start5 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            ids[i] = new UUID22().toHex();
        }
        final long stop5 = System.currentTimeMillis();
        for (int i = 1; i < n; i++) {
            assertTrue(!ids[i - 1].equals(ids[i]));
        }
        System.out.println("B32: " + (stop - start) + " vsHex: " + (stop2 - start2) + " vsB32: " + (stop4 - start4) + " vxHex: "
                + (stop5 - start5));
    }

    @Test
    public void testGetBytesImmutability() {
        final UUID22 id = new UUID22();
        final byte[] bytes = id.getBytes();
        final byte[] original = Arrays.copyOf(bytes, bytes.length);
        bytes[0] = 0;
        bytes[1] = 0;
        bytes[2] = 0;

        assertTrue(Arrays.equals(id.getBytes(), original));
    }

    @Test
    public void testConstructorImmutability() {
        final UUID22 id = new UUID22();
        final byte[] bytes = id.getBytes();
        final byte[] original = Arrays.copyOf(bytes, bytes.length);

        try {
            final UUID22 id2 = new UUID22(bytes);
            bytes[0] = 0;
            bytes[1] = 0;

            assertTrue(Arrays.equals(id2.getBytes(), original));
        } catch (final InvalidUuidOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testVersionField() {
        final UUID22 generated = new UUID22();
        assertEquals(VERSION, generated.getVersion());

        try {
            final UUID22 parsed1 = new UUID22("aeaqaaaaaitxll67abarqaktftcfyniaaaaq");
            assertEquals(VERSION, parsed1.getVersion());
        } catch (final InvalidUuidOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testHexBase32() {
        try {
            final UUID22 parsed1 = new UUID22("aeaqaaaaaitxll67abarqaktftcfyniaaaaq");
            final UUID22 parsed2 = new UUID22("0101000000022775afdf00411801532cc45c35000001");
            final UUID22 parsed0 = new UUID22("AQEAAAACJ3Wv3wBBGAFTLMRcNQAAAQ");
            assertTrue(parsed1.equals(parsed2));
            assertTrue(parsed1.equals(parsed0));
            final UUID22 generated = new UUID22();
            final UUID22 parsed3 = new UUID22(generated.getBytes());
            final UUID22 parsed4 = new UUID22(generated.toBase32());
            final UUID22 parsed5 = new UUID22(generated.toHex());
            final UUID22 parsed6 = new UUID22(generated.toString());
            final UUID22 parsed7 = new UUID22(generated.toBase64());
            assertTrue(generated.equals(parsed3));
            assertTrue(generated.equals(parsed4));
            assertTrue(generated.equals(parsed5));
            assertTrue(generated.equals(parsed6));
            assertTrue(generated.equals(parsed7));
        } catch (final InvalidUuidOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void testPIDField() throws Exception {
        final UUID22 id = new UUID22();
        assertEquals(UUID22.jvmProcessId(), id.getProcessId());
    }

    @Test
    public void testDateField() {
        final UUID22 id = new UUID22();
        System.out.println(id.getTimestamp());
        System.out.println(new Date().getTime());
        assertTrue(id.getTimestamp() > new Date().getTime() - 100);
        assertTrue(id.getTimestamp() < new Date().getTime() + 100);
    }

    @Test
    public void testObjectIdField() {
        UUID22 id = new UUID22();
        assertEquals(0, id.getObjectId());
        for (int i = 0; i < 32; i++) {
            id = new UUID22(i, 0);
            assertEquals(i, id.getObjectId());
        }
    }

    @Test
    public void testDomainIdField() {
        UUID22 id = new UUID22();
        assertEquals(0, id.getDomainId());
        for (int i = 0; i < 65535; i++) {
            id = new UUID22(0, i);
            assertEquals(i, id.getDomainId());
        }
        for (int i = 0; i < 255; i++) {
        	id = new UUID22(0, 0x3FFFFFFF - i);
            assertEquals(0x3FFFFFFF - i, id.getDomainId());
        }
    }

    @Test
    public void testPlatformIdField() {
        UUID22 id = new UUID22();
        assertEquals(UUID22.MACI, id.getPlatformId());
        for (int i = 0; i < 65535; i++) {
            id = new UUID22(0, 0, i);
            assertEquals(i, id.getPlatformId());
            assertFalse(id.isWorm());
        }
        for (int i = 0; i < 255; i++) {
            id = new UUID22(0, 0, 0x7FFFFFFF - i);
            assertEquals(0x7FFFFFFF - i, id.getPlatformId());
            assertFalse(id.isWorm());
        }
        // WORM
        for (int i = 0; i < 65535; i++) {
            id = new UUID22(0, 0, i, true);
            assertEquals(i, id.getPlatformId());
            assertTrue(id.isWorm());
        }
        for (int i = 0; i < 255; i++) {
            id = new UUID22(0, 0, 0x7FFFFFFF - i, true);
            assertEquals(0x7FFFFFFF - i, id.getPlatformId());
            assertTrue(id.isWorm());
        }
    }

    @Test
    public void testForDuplicates() {
        final int n = NB;
        final Set<UUID22> UUIDs = new HashSet<UUID22>();
        final UUID22[] UUIDArray = new UUID22[n];

        final long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            UUIDArray[i] = new UUID22();
        }
        final long stop = System.currentTimeMillis();
        System.out.println("TimeSequential = " + (stop - start) + " so " + (n * 1000 / (stop - start)) + " UUID22s/s");

        for (int i = 0; i < n; i++) {
            UUIDs.add(UUIDArray[i]);
        }

        System.out.println("Create " + n + " and get: " + UUIDs.size());
        assertEquals(n, UUIDs.size());
        checkConsecutive(UUIDArray);
    }
    
    @Test
    public void checkShuffleInteger() {
        byte[] uuid = new byte[4];
        for (int src = 0; src < NB ; src++) {
            uuid[1] = (byte) (src & 0xFF);
            uuid[2] = (byte) ((src >> 8) & 0xFF);
            uuid[3] = (byte) ((src >> 16) & 0xFF);
            int count = (uuid[3] & 0xFF) << 16;
            count |= (uuid[2] & 0xFF) << 8;
            count |= uuid[1] & 0xFF;
            assertEquals(src, count);
        }
    }
    
    /**
     * See FsDigitalObject and FsProperty
     */
    @Test
    public void testUuidToPath() {
    	/**
         * Key factor
         */
        final int FACTOR = 3;
        /**
         * KEY Size
         */
        final int KEYSIZE = UUID22.KEYSIZE;
        /**
         * Default depth (from UUID KEYSIZE)
         */
        final int DEPTH = KEYSIZE / FACTOR;
        /**
         * Length for one key (256^FACTOR in decimal representation)
         */
        final int DECIMAL_KEY_LENGTH = 8;
        /**
         * Key cast
         */
        final int CAST = 0xFFFFFF;
        /**
         * Length for last key after depth (256^LASTKEY_FACTOR = 4 in decimal representation).
         * So filesystem will have a new directory possibly every 1/4 s instead of every ms
         */
        final int DECIMAL_LASTKEY_LENGTH = 10;
        /**
         * Last Key cast
         */
        final long LASTCAST = 0xFFFFFFFFL;
        /**
         * Last Key modulo
         */
        final int LASTKEY_MODULO = 256;
        /**
         * Last Factor
         */
        final int LASTFACTOR = KEYSIZE - ((DEPTH - 1) * FACTOR);
        for (int t = 0; t < NB/10; t++) {
	    	final UUID22 id1 = new UUID22();
	    	final byte[] bytes = id1.getBytes();
	    	final String formatDefault = "%0"+DECIMAL_KEY_LENGTH+"d";
	    	final String formatLast = "%0"+DECIMAL_LASTKEY_LENGTH+"d";
	    	StringBuilder bpath = new StringBuilder(
	                DEPTH * (DECIMAL_KEY_LENGTH + 1) +
	                (DECIMAL_LASTKEY_LENGTH + 1));
	        int j = 0;
	        for (int i = 0; i < DEPTH - 1; i++) {
	            bpath.append(File.separatorChar);
	            int b = 0;
	            for(int k = FACTOR - 1; k >= 0; k--, j++) {
	                b += ((bytes[j] & 0xFF) << (k*8));
	            }
	            // Locale.US to prevent strange behavior like with Arabic
	            bpath.append(String.format(Locale.US, formatDefault, b));
	        }
	        bpath.append(File.separatorChar);
	        long b = 0;
	        for (int k = LASTFACTOR - 1; j < bytes.length; k--, j++) {
	        	b += ((long) (bytes[j] & 0xFF)) << (k*8);
	        }
	        // Locale.US to prevent strange behavior like with Arabic
	        final String spath = bpath.append(String.format(Locale.US, formatLast, b)).toString();
	        final Path path = new File(spath).toPath();
	        final int initialLevel = path.getNameCount() - DEPTH;
	        int[] iuuid = new int[DEPTH + 1];
	        try {
	            for (int i = 0; i < DEPTH - 1; i++) {
	                iuuid[i] =
	                        (Integer.parseInt(
	                                path.getName(initialLevel+i).toString())
	                                & CAST);
	            }
	            // Convert from filename to long
	            final String filename = path.getFileName().toString();
	            final long value = 
	                    (Long.parseLong(
	                            filename)
	                            & LASTCAST);
	            // Convert long to 2 int
	            iuuid[DEPTH - 1] = (int) (value / LASTKEY_MODULO);
	            iuuid[DEPTH] = (int) (value % LASTKEY_MODULO);
	        } catch (NumberFormatException e) {
	        	e.printStackTrace();
	            fail(e.getMessage());
	        }
	        byte[] buuid = new byte[KEYSIZE];
	        j = 0;
	        // Convert from int to byte
	        for (int i = 0; i < DEPTH; i++) {
	            for (int k = FACTOR - 1; k >= 0; k--, j++) {
	                buuid[j] = (byte) ((iuuid[i] >> (k*8)) & 0xFF);
	            }
	        }
	        // Convert from int to byte for last part out of DEPTH
	        for (int k = LASTFACTOR - FACTOR - 1;
	        		j < KEYSIZE; k--, j++) {
	            buuid[j] = (byte) ((iuuid[DEPTH] >> (k*8)) & 0xFF);
	        }
	        assertTrue(Arrays.equals(bytes, buuid));
	        UUID22 id2;
			try {
				id2 = new UUID22(buuid);
		        assertEquals(id1, id2);
			} catch (InvalidUuidOperationException e) {
				e.printStackTrace();
	            fail(e.getMessage());
			}
        }
    }

    private void checkConsecutive(final UUID22[] UUIDArray) {
        final int n = UUIDArray.length;
        int i = 1;
        int largest = 0;
        for (; i < n; i++) {
            if (UUIDArray[i].getTimestamp() > UUIDArray[i - 1].getTimestamp()) {
                int j = i + 1;
                final long time = UUIDArray[i].getTimestamp();
                for (; j < n; j++) {
                    if (UUIDArray[i].compareTo(UUIDArray[j]) != -1) {
                        for (int k = i; k <= j; k++) {
                            //System.out.println(k+"="+UUIDArray[k].getId()+":"+UUIDArray[k].getCounter()+":"+UUIDArray[k].getTimestamp()+":"+UUIDArray[k].getVersion()+":"+UUIDArray[k].getProcessId());
                        }
                    }
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
                if (UUIDArray[i-1].compareTo(UUIDArray[i]) != -1) {
                    for (int k = i-1; k <= i; k++) {
                        //System.out.println(k+"="+UUIDArray[k].getId()+":"+UUIDArray[k].getCounter()+":"+UUIDArray[k].getTimestamp()+":"+UUIDArray[k].getVersion()+":"+UUIDArray[k].getProcessId());
                    }
                }
                assertEquals(-1, UUIDArray[i-1].compareTo(UUIDArray[i]));
                int j = i + 1;
                final long time = UUIDArray[i].getTimestamp();
                for (; j < n; j++) {
                    if (UUIDArray[i-1].compareTo(UUIDArray[j]) != -1) {
                        for (int k = i-1; k <= j; k++) {
                            //System.out.println(k+"="+UUIDArray[k].getId()+":"+UUIDArray[k].getCounter()+":"+UUIDArray[k].getTimestamp()+":"+UUIDArray[k].getVersion()+":"+UUIDArray[k].getProcessId());
                        }
                    }
                    assertEquals(-1, UUIDArray[i-1].compareTo(UUIDArray[j]));
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
        System.out.println(largest + " different consecutive elements");
    }

    private static class Generator extends Thread {
        private final UUID22[] UUIDs;
        int base;
        int n;

        public Generator(final int n, final UUID22[] UUIDs, final int base) {
            this.n = n;
            this.UUIDs = UUIDs;
            this.base = base;
        }

        @Override
        public void run() {
            for (int i = 0; i < n; i++) {
                UUIDs[base + i] = new UUID22();
            }
        }
    }

    @Test
    public void testConcurrentGeneration() throws Exception {
        final int numThreads = Runtime.getRuntime().availableProcessors() + 1;
        final Thread[] threads = new Thread[numThreads];
        final int n = NB * 2;
        final int step = n / numThreads;
        final UUID22[] UUIDs = new UUID22[step * numThreads];

        final long start = System.currentTimeMillis();
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Generator(step, UUIDs, i * step);
            threads[i].start();
        }

        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        final long stop = System.currentTimeMillis();

        final Set<UUID22> UUIDSet = new HashSet<UUID22>();

        for (int i = 0; i < UUIDs.length; i++) {
            UUIDSet.add(UUIDs[i]);
        }

        assertEquals(UUIDs.length, UUIDSet.size());
        UUIDSet.clear();
        System.out.println("TimeConcurrent = " + (stop - start) + " so " + (UUIDs.length * 1000 / (stop - start)) + " UUID22s/s");
        final TreeSet<UUID22> set = new TreeSet<>();
        for (int i = 0; i < UUIDs.length; i++) {
            set.add(UUIDs[i]);
        }
        checkConsecutive(set.toArray(new UUID22[0]));
    }
    
    @Test
    public void testJsonXml() {
        UUID22 uuid = new UUID22(1, 2);
    	System.out.println(uuid.toHex());
    	System.out.println(uuid.toBase32());
    	System.out.println(uuid.toBase64());
        try {
            String json = JsonHandler.writeAsString(uuid);
            System.out.println(json);
            UUID22 uuid2 = JsonHandler.getFromString(json, UUID22.class);
            assertEquals("Json check", uuid, uuid2);
            String xml = XmlHandler.writeAsString(uuid);
            System.out.println(xml);
            UUID22 uuid3 = XmlHandler.getFromString(xml, UUID22.class);
            assertEquals("Xml check", uuid, uuid3);
        } catch (InvalidParseOperationException e) {
            e.printStackTrace();
            fail("Exception occurs: "+e.getMessage());
            return;
        }
    }
}