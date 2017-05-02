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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Tes for MultipleInputStreamHandler
 */
public class MultipleInputStreamHandlerTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultipleInputStreamHandlerTest.class);
    private static final int INPUTSTREAM_SIZE = 65536 * 4;
    private static final TreeMap<Long, String> TIMES = new TreeMap<>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

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
        LOGGER.warn("start bad initialization Pool {}", MultipleInputStreamHandler.getPoolAvailability());

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
            // ignore
        }
        LOGGER.warn("end bad initialization Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testMultipleInputStreamHandlerSingle() {
        final long start = System.nanoTime();
        LOGGER.warn("start testMultipleInputStreamHandlerSingle Pool {}", MultipleInputStreamHandler.getPoolAvailability());

        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
                MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, 1)) {
            assertNotNull(mish.toString());
            final InputStream is = mish.getInputStream(0);
            long total = 0;
            assertNotNull(is);
            while (is.read() >= 0) {
                total++;
            }
            assertEquals(INPUTSTREAM_SIZE, total);
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read 1: \t{} ns", stop - start);
        LOGGER.warn("end testMultipleInputStreamHandlerSingle Pool {}", MultipleInputStreamHandler.getPoolAvailability());

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
            while ((read = is.read(buffer)) >= 0) {
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
        LOGGER.warn("start testMultipleInputStreamHandlerBlock Pool {}", MultipleInputStreamHandler.getPoolAvailability());
        testMultipleInputStreamHandlerBlock(100);
        testMultipleInputStreamHandlerBlock(512);
        testMultipleInputStreamHandlerBlock(1024);
        testMultipleInputStreamHandlerBlock(4000);
        testMultipleInputStreamHandlerBlock(8192);
        testMultipleInputStreamHandlerBlock(40000);
        testMultipleInputStreamHandlerBlock(65536);
        testMultipleInputStreamHandlerBlock(80000);
        testMultipleInputStreamHandlerBlock(100000);
        LOGGER.warn("stop testMultipleInputStreamHandlerBlock Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testMultipleInputStreamHandlerMultipleShift8K() {
        final long start = System.nanoTime();
        final int size = 8192;
        final int nb = 10;
        LOGGER.warn("start testMultipleInputStreamHandlerMultipleShift8K Pool {}", MultipleInputStreamHandler.getPoolAvailability());
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
        LOGGER.warn("stop testMultipleInputStreamHandlerMultipleShift8K Pool {}", MultipleInputStreamHandler.getPoolAvailability());
        addTimer(stop - start, "MULTIPLE_BLOCK_" + size + "_" + nb + " :\t" + (stop - start) + "  \t" + (stop - start) / nb);
    }

    private static class ThreadReader implements Callable<Integer> {
        private final InputStream is;
        private final int size;
        private final int rank;

        private ThreadReader(int rank, InputStream is, int size) {
            this.rank = rank;
            this.is = is;
            this.size = size;
        }

        @Override
        public Integer call() {
            int read;
            int total = 0;
            final byte[] buffer = new byte[size];
            try {
                while ((read = is.read(buffer)) >= 0) {
                    LOGGER.debug("{} Read: {}", rank,read);
                    total += read;
                }
                LOGGER.debug("{} Read: {} Total: {}", rank, read, total);
                return total;
            } catch (final IOException e) {
                LOGGER.error(e);
                return total;
            }
        }

    }

    private void testMultipleInputStreamHandlerMultipleMultiThread(int nb, int size, boolean block, boolean timer) {
        final long start = System.nanoTime();
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, block);
                MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, nb)) {
            InputStream is;
            @SuppressWarnings("unchecked")
            final Future<Integer>[] total = new Future[nb];
            final ExecutorService executor = Executors.newFixedThreadPool(nb);
            for (int i = 0; i < nb; i++) {
                is = mish.getInputStream(i);
                final ThreadReader threadReader = new ThreadReader(i, is, size);
                total[i] = executor.submit(threadReader);
            }
            executor.shutdown();
            while (!executor.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
                ;
            }
            for (int i = 0; i < nb; i++) {
                assertEquals(INPUTSTREAM_SIZE, (int) total[i].get());
            }
        } catch (final InterruptedException | ExecutionException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read {}: \t{} ns", size, stop - start);
        if (timer) {
            addTimer((stop - start) / nb, "PARALLEL_VAR_SIZE_" + (block ? "BLOCK_" : "BYTE_") + size + "_" + nb + " :\t"
                    + (stop - start) + "  \t" + (stop - start) / nb);
        }
    }

    @Test
    public void testMultipleInputStreamHandlerMultipleMultiThread() {
        LOGGER.warn("start testMultipleInputStreamHandlerMultipleMultiThread Pool {}", MultipleInputStreamHandler.getPoolAvailability());
        testMultipleInputStreamHandlerMultipleMultiThread(1, 8192, true, true);
        testMultipleInputStreamHandlerMultipleMultiThread(1, 8192, true, true);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 8192, true, true);
        testMultipleInputStreamHandlerMultipleMultiThread(1, 65536, true, true);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 65536, true, true);

        testMultipleInputStreamHandlerMultipleMultiThread(1, 8192, false, true);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 8192, false, true);
        testMultipleInputStreamHandlerMultipleMultiThread(1, 65536, false, true);
        testMultipleInputStreamHandlerMultipleMultiThread(10, 65536, false, true);
        LOGGER.warn("stop testMultipleInputStreamHandlerMultipleMultiThread Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testMultipleInputStreamHandlerMultiRead() {
        LOGGER.warn("start testMultipleInputStreamHandlerMultiRead Pool {}", MultipleInputStreamHandler.getPoolAvailability());
        for (int i = 0; i < 1002; i++) {
            if (i % 100 == 0) {
                LOGGER.warn("Step {} Pool: {}", i, MultipleInputStreamHandler.getPoolAvailability());
            }
            testMultipleInputStreamHandlerMultipleMultiThread(1, 1024, true, false);
        }
        LOGGER.warn("stop testMultipleInputStreamHandlerMultiRead Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testMultipleInputStreamHandlerMultipleMultiThreadWithVariableSizes() {
        LOGGER.warn("start testMultipleInputStreamHandlerMultipleMultiThreadWithVariableSizes Pool {}", MultipleInputStreamHandler.getPoolAvailability());
        for (int len = 100; len < 2200; len += 500) {
            testMultipleInputStreamHandlerMultipleMultiThread(1, len, true, false);
            testMultipleInputStreamHandlerMultipleMultiThread(10, len, true, false);
    
            testMultipleInputStreamHandlerMultipleMultiThread(1, len, false, false);
            testMultipleInputStreamHandlerMultipleMultiThread(10, len, false, false);
        }
        for (int len = 100; len < 80000; len += 10000) {
            testMultipleInputStreamHandlerMultipleMultiThread(1, len, true, false);
            testMultipleInputStreamHandlerMultipleMultiThread(10, len, true, false);

            testMultipleInputStreamHandlerMultipleMultiThread(1, len, false, false);
            testMultipleInputStreamHandlerMultipleMultiThread(10, len, false, false);
        }
        LOGGER.warn("stop testMultipleInputStreamHandlerMultipleMultiThreadWithVariableSizes Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testClose() {
        final int size = 8192;
        final int nb = 10;
        LOGGER.warn("start testClose Pool {}", MultipleInputStreamHandler.getPoolAvailability());
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, true);
                MultipleInputStreamHandler mish = new MultipleInputStreamHandler(fakeInputStream, nb)) {
            final InputStream[] is = new InputStream[nb];
            for (int i = 0; i < nb; i++) {
                is[i] = mish.getInputStream(i);
            }
            LOGGER.warn("current testClose Pool {}", MultipleInputStreamHandler.getPoolAvailability());
            final byte[] buffer = new byte[size];
            int rank = 1;
            for (int i = 0; i < 100; i++) {
                rank = (rank + 1) % nb;
                if (is[rank].read(buffer) < 0) {
                    break;
                }
            }
            mish.close();
            for (int i = 0; i < nb; i++) {
                assertEquals("rank: " + i, -1, is[i].available());
            }
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        LOGGER.warn("stop testClose Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testMultipleClose() {
        final int size = 8192;
        final int nb = 10;
        LOGGER.warn("start testMultipleClose Pool {}", MultipleInputStreamHandler.getPoolAvailability());
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
        LOGGER.warn("stop testMultipleClose Pool {}", MultipleInputStreamHandler.getPoolAvailability());
    }

    @Test
    public void testConcurrentMultipleIntputStreamHandler() {
        int old = VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM;
        VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM = 2000;
        
        List<FakeInputStream> listStream = new ArrayList<>(VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER + 1);
        try {
            List<MultipleInputStreamHandler> list = new ArrayList<>(VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER);
            LOGGER.warn("start allocate stream {}", MultipleInputStreamHandler.getPoolAvailability());
            for (int i = 0; i < VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER + 1; i++) {
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE, true));
            }
            LOGGER.warn("start allocate MISH {}", MultipleInputStreamHandler.getPoolAvailability());
            for (int i = 0; i < VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER; i++) {
                try {
                    MultipleInputStreamHandler mish = new MultipleInputStreamHandler(listStream.get(i), 1);
                    list.add(mish);
                    LOGGER.debug(mish.toString());
                } catch (IllegalArgumentException e) {
                    fail("Should not be interrupted");
                }
            }
            // Try to allocate once more but not possible
            LOGGER.warn("start allocate MISH 1 more than possible {}", MultipleInputStreamHandler.getPoolAvailability());
            try {
                list.add(new MultipleInputStreamHandler(listStream.get(VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER), 1));
                fail("Should be interrupted");
            } catch (IllegalArgumentException e) {
                // legal
            }
            LOGGER.warn("start free half of MISH and streams and reallocate streams {}",
                    MultipleInputStreamHandler.getPoolAvailability());
            // Now free half of the list
            for (int i = VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER - 1; i >= 500; i--) {
                MultipleInputStreamHandler mish = list.remove(i);
                mish.close();
                LOGGER.debug(mish.toString());
                StreamUtils.closeSilently(listStream.remove(i));
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE, true));
            }
            // Now reallocate 500
            LOGGER.warn("start half of MISH {}", MultipleInputStreamHandler.getPoolAvailability());
            for (int i = 500; i < VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER; i++) {
                try {
                    MultipleInputStreamHandler mish = new MultipleInputStreamHandler(listStream.get(i), 1);
                    list.add(mish);
                    LOGGER.debug(mish.toString());
                } catch (IllegalArgumentException e) {
                    fail("Should not be interrupted");
                }
            }
            LOGGER.warn("Closing all {}", MultipleInputStreamHandler.getPoolAvailability());
            for (MultipleInputStreamHandler mish : list) {
                mish.close();
                LOGGER.debug(mish.toString());
            }
            for (FakeInputStream fakeInputStream : listStream) {
                StreamUtils.closeSilently(fakeInputStream);
            }
            LOGGER.warn("Restart from 0 with 1000 {}", MultipleInputStreamHandler.getPoolAvailability());
            for (int i = 0; i < VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER; i++) {
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE, true));
            }
            LOGGER.warn("Try reading {}", MultipleInputStreamHandler.getPoolAvailability());
            for (int i = 0; i < VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER; i++) {
                try {
                    MultipleInputStreamHandler mish = new MultipleInputStreamHandler(listStream.get(i), 1);
                    LOGGER.debug(mish.toString());
                    InputStream stream = mish.getInputStream(0);
                    StreamUtils.closeSilently(stream);
                    mish.close();
                    LOGGER.debug(mish.toString());
                } catch (IllegalArgumentException e) {
                    fail("Should not be interrupted");
                }
            }
            LOGGER.warn("End of test {}", MultipleInputStreamHandler.getPoolAvailability());
        } finally {
            for (FakeInputStream fakeInputStream : listStream) {
                StreamUtils.closeSilently(fakeInputStream);
            }
            VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM = old;
        }
    }
    @Test
    public void testConcurrentMultipleThreadIntputStreamHandler() {
        int old = VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM;
        VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM = 2000;
        
        int nb = VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER;
        List<FakeInputStream> listStream = new ArrayList<>(nb + 1);
        try {
            List<MultipleInputStreamHandler> list = new ArrayList<>(nb);
            LOGGER.warn("Restart MultiThread from 0 with {} {}", nb, MultipleInputStreamHandler.getPoolAvailability());
            for (int i = 0; i < nb; i++) {
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE, true));
            }
            LOGGER.warn("Start real concurrent tests {}", MultipleInputStreamHandler.getPoolAvailability());
            final ExecutorService executor = Executors.newFixedThreadPool(nb);
            list.clear();
            @SuppressWarnings("unchecked")
            final Future<Integer>[] total = new Future[nb];
            for (int i = 0; i < nb; i++) {
                MultipleInputStreamHandler mish = new MultipleInputStreamHandler(listStream.get(i), 1);
                list.add(mish);
                LOGGER.debug(mish.toString());
                InputStream stream = mish.getInputStream(0);
                final ThreadReader threadReader = new ThreadReader(i, stream, VitamConfiguration.getChunkSize());
                total[i] = executor.submit(threadReader);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                // ignore
            }
            executor.shutdown();
            try {
                while (!executor.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
                    ;
                }
            } catch (InterruptedException e) {
                LOGGER.error(e);
                fail("Should not failed");
            }
            for (int i = 0; i < nb; i++) {
                try {
                    assertEquals("Rank not equel: " + i, INPUTSTREAM_SIZE, (int) total[i].get());
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error(e);
                    fail("Should not failed");
                }
            }
            for (MultipleInputStreamHandler mish : list) {
                mish.close();
            }
            LOGGER.warn("End of test {}", MultipleInputStreamHandler.getPoolAvailability());

        } finally {
            for (FakeInputStream fakeInputStream : listStream) {
                StreamUtils.closeSilently(fakeInputStream);
            }
            VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM = old;
        }
    }

}
