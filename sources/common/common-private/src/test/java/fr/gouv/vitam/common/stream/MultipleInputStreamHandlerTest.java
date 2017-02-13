/**
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
 */
package fr.gouv.vitam.common.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Tes for MultipleInputStreamHandler
 */
public class MultipleInputStreamHandlerTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultipleInputStreamHandlerTest.class);
    private static final int INPUTSTREAM_SIZE = 100000;
    private static final TreeMap<Long, String> TIMES = new TreeMap<>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void endingClass() {
        final StringBuilder builder = new StringBuilder("Time Results:");
        TIMES.forEach(new BiConsumer<Long, String>() {
            @Override
            public void accept(Long t, String u) {
                builder.append("\n\t").append(u);
            }
        });
        LOGGER.warn(builder.toString());
    }

    private void addTimer(long time, String result) {
        while (TIMES.containsKey(time)) {
            time++;
        }
        TIMES.put(time, time + " = " + result);
    }

    @Test
    public void badInitialization() {
        try (MultipleInputStreamHandler mish = new MultipleInputStreamHandler(null, 1)) {
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException e) {
            // nothing
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, 0)) {
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException e) {
            // nothing
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, 1)) {
            mish.getInputStream(-1);
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException e) {
            // nothing
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, 1)) {
            mish.getInputStream(1);
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException e) {
            // nothing
        }
    }

    @Test
    public void testMultipleInputStreamHandlerSingle() {
        final long start = System.nanoTime();
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, 1)) {
            assertNotNull(mish.toString());
            final InputStream is = mish.getInputStream(0);
            long total = 0;
            assertNotNull(is.toString());
            while (is.read() >= 0) {
                total++;
            }
            assertEquals(INPUTSTREAM_SIZE, total);
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read 1: \t{} ns", stop - start);
        addTimer(stop - start, "SINGLE_BYTE :\t" + (stop - start));
    }

    private void testMultipleInputStreamHandlerBlock(int size) {
        final long start = System.nanoTime();
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, 1)) {
            final InputStream is = mish.getInputStream(0);
            int read;
            long total = 0;
            final byte[] buffer = new byte[size];
            while ((read = is.read(buffer)) > 0) {
                total += read;
            }
            assertEquals(INPUTSTREAM_SIZE, total);
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read {}: \t{} ns", size, stop - start);
        addTimer(stop - start, "SINGLE_BLOCK_" + size + "  :\t" + (stop - start));
    }

    @Test
    public void testMultipleInputStreamHandlerBlock() {
        testMultipleInputStreamHandlerBlock(100);
        testMultipleInputStreamHandlerBlock(512);
        testMultipleInputStreamHandlerBlock(1024);
        testMultipleInputStreamHandlerBlock(4000);
        testMultipleInputStreamHandlerBlock(8192);
        testMultipleInputStreamHandlerBlock(40000);
        testMultipleInputStreamHandlerBlock(65536);
        testMultipleInputStreamHandlerBlock(80000);
        testMultipleInputStreamHandlerBlock(100000);
    }

    @Test
    public void testMultipleInputStreamHandlerMultipleShift8K() {
        final long start = System.nanoTime();
        final int size = 8192;
        final int nb = 10;
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, nb)) {
            final InputStream[] is = new InputStream[nb];
            final long[] total = new long[nb];
            for (int i = 0; i < nb; i++) {
                is[i] = mish.getInputStream(i);
                total[i] = 0;
            }
            int read;
            final byte[] buffer = new byte[size];
            int rank = 1;
            while (total[0] < INPUTSTREAM_SIZE) {
                rank = (rank + 1) % nb;
                if ((read = is[rank].read(buffer)) > 0) {
                    total[rank] += read;
                }
            }
            for (int i = 0; i < nb; i++) {
                while ((read = is[i].read(buffer)) > 0) {
                    total[i] += read;
                }
                assertEquals("rank: " + i, INPUTSTREAM_SIZE, total[i]);
            }
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read {}: \t{} ns", size, stop - start);
        addTimer(stop - start,
            "MULTIPLE_BLOCK_" + size + "_" + nb + " :\t" + (stop - start) + "  \t" + (stop - start) / nb);
    }

    private static class ThreadReader implements Runnable {
        private final InputStream is;
        private final int size;
        private final long[] total;
        private final int rank;

        private ThreadReader(int rank, long[] total, InputStream is, int size) {
            this.rank = rank;
            this.total = total;
            this.is = is;
            this.size = size;
        }

        @Override
        public void run() {
            int read;
            total[rank] = 0;
            final byte[] buffer = new byte[size];
            try {
                while ((read = is.read(buffer)) > 0) {
                    total[rank] += read;
                }
            } catch (final IOException e) {
                LOGGER.error(e);
                return;
            }
        }
    }

    private void testMultipleInputStreamHandlerMultipleMultiThread(int nb, int size, boolean block) {
        final long start = System.nanoTime();
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, block);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, nb)) {
            InputStream is;
            final long[] total = new long[nb];
            final ExecutorService executor = Executors.newFixedThreadPool(nb);
            for (int i = 0; i < nb; i++) {
                is = mish.getInputStream(i);
                final ThreadReader threadReader = new ThreadReader(i, total, is, size);
                executor.execute(threadReader);
            }
            executor.shutdown();
            while (!executor.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
                ;
            }
            for (int i = 0; i < nb; i++) {
                assertEquals(INPUTSTREAM_SIZE, total[i]);
            }
        } catch (final InterruptedException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read {}: \t{} ns", size, stop - start);

        addTimer((stop - start) / nb, "PARALLEL_VAR_SIZE_" + (block ? "BLOCK_" : "BYTE_") + size + "_" + nb + " :\t" +
            (stop - start) + "  \t" + (stop - start) / nb);
    }

    @Test
    public void testMultipleInputStreamHandlerMultipleMultiThread() {
        testMultipleInputStreamHandlerMultipleMultiThread(1, 8192, true);
        testMultipleInputStreamHandlerMultipleMultiThread(1, 8192, true);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 8192, true);
        testMultipleInputStreamHandlerMultipleMultiThread(1, 65536, true);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 65536, true);

        testMultipleInputStreamHandlerMultipleMultiThread(1, 8192, false);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 8192, false);
        testMultipleInputStreamHandlerMultipleMultiThread(1, 65536, false);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 65536, false);
    }

    @Test
    public void testMultipleInputStreamHandlerMultipleMultiThreadWithVariableSizes() {
        for (int len = 100; len < 80000; len += 500) {
            testMultipleInputStreamHandlerMultipleMultiThread(1, len, true);
            testMultipleInputStreamHandlerMultipleMultiThread(10, len, true);

            testMultipleInputStreamHandlerMultipleMultiThread(1, len, false);
            testMultipleInputStreamHandlerMultipleMultiThread(10, len, false);
        }
    }

    @Test
    public void testClose() {
        final int size = 8192;
        final int nb = 10;
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, nb)) {
            final InputStream[] is = new InputStream[nb];
            for (int i = 0; i < nb; i++) {
                is[i] = mish.getInputStream(i);
            }
            final byte[] buffer = new byte[size];
            int rank = 1;
            for (int i = 0; i < 100; i++) {
                rank = (rank + 1) % nb;
                is[rank].read(buffer);
            }
            mish.close();
            for (int i = 0; i < nb; i++) {
                assertEquals("rank: " + i, -1, is[i].available());
            }
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleClose() {
        final int size = 8192;
        final int nb = 10;
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
            MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, nb)) {
            final InputStream[] is = new InputStream[nb];
            for (int i = 0; i < nb; i++) {
                is[i] = mish.getInputStream(i);
            }
            final byte[] buffer = new byte[size];
            int rank = 1;
            for (int i = 0; i < 100; i++) {
                rank = (rank + 1) % nb;
                is[rank].read(buffer);
            }
            for (int i = 0; i < nb; i++) {
                is[i].close();
                assertEquals("rank: " + i, -1, is[i].available());
            }
            mish.close();
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
    }

}
