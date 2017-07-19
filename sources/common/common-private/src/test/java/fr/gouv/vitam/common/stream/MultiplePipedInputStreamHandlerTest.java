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
 * Tes for MultiplePipedInputStream
 */
public class MultiplePipedInputStreamHandlerTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultiplePipedInputStreamHandlerTest.class);
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

        try (MultiplePipedInputStream mish = new MultiplePipedInputStream(null, 1)) {
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException | IOException e) {
            // nothing
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, 0)) {
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException | IOException e) {
            // nothing
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, 1)) {
            mish.getInputStream(-1);
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException | IOException e) {
            // nothing
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, 1)) {
            mish.getInputStream(1);
            fail("Should raized illegal argument");
        } catch (final IllegalArgumentException | IOException e) {
            // ignore
        }
    }

    @Test
    public void testMultiplePipedInputStreamSingle() {
        final long start = System.nanoTime();

        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, 1)) {
            assertNotNull(mish.toString());
            final InputStream is = mish.getInputStream(0);
            long total = 0;
            assertNotNull(is);
            while (is.read() >= 0) {
                total++;
            }
            assertEquals(INPUTSTREAM_SIZE, total);
            mish.hasException();
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read 1: \t{} ns", stop - start);

        addTimer(stop - start, "SINGLE_BYTE :\t" + (stop - start));
    }

    private void testMultiplePipedInputStreamBlock(int size) {
        final long start = System.nanoTime();
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, 1)) {
            final InputStream is = mish.getInputStream(0);
            int read;
            long total = 0;
            final byte[] buffer = new byte[size];
            while ((read = is.read(buffer)) >= 0) {
                total += read;
            }
            assertEquals(INPUTSTREAM_SIZE, total);
            mish.hasException();
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read {}: \t{} ns", size, stop - start);
        addTimer(stop - start, "SINGLE_BLOCK_" + size + "  :\t" + (stop - start));
    }

    @Test
    public void testMultiplePipedInputStreamBlock() {
        testMultiplePipedInputStreamBlock(100);
        testMultiplePipedInputStreamBlock(512);
        testMultiplePipedInputStreamBlock(1024);
        testMultiplePipedInputStreamBlock(4000);
        testMultiplePipedInputStreamBlock(8192);
        testMultiplePipedInputStreamBlock(40000);
        testMultiplePipedInputStreamBlock(65536);
        testMultiplePipedInputStreamBlock(80000);
        testMultiplePipedInputStreamBlock(100000);
    }

    @Test
    public void testMultiplePipedInputStreamMultipleShift8K() {
        final long start = System.nanoTime();
        final int size = 8192;
        final int nb = 10;
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, nb)) {
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
            mish.hasException();
        } catch (final IOException e) {
            System.gc();
            LOGGER.error(e);
            fail("Should not raized an exception: " + e.getMessage());
        }
        final long stop = System.nanoTime();
        LOGGER.debug("Read {}: \t{} ns", size, stop - start);
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

    private void testMultiplePipedInputStreamMultipleMultiThread(int nb, int size, boolean block, boolean timer) {
        final long start = System.nanoTime();
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE, block);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, nb)) {
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
            mish.hasException();
        } catch (final InterruptedException | ExecutionException | IOException e) {
            LOGGER.error(e);
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
    public void testMultiplePipedInputStreamMultipleMultiThread() {
        testMultiplePipedInputStreamMultipleMultiThread(1, 8192, true, true);
        testMultiplePipedInputStreamMultipleMultiThread(1, 8192, true, true);
        testMultiplePipedInputStreamMultipleMultiThread(10, 8192, true, true);
        testMultiplePipedInputStreamMultipleMultiThread(1, 65536, true, true);
        testMultiplePipedInputStreamMultipleMultiThread(10, 65536, true, true);

        testMultiplePipedInputStreamMultipleMultiThread(1, 8192, false, true);
        testMultiplePipedInputStreamMultipleMultiThread(10, 8192, false, true);
        testMultiplePipedInputStreamMultipleMultiThread(1, 65536, false, true);
        testMultiplePipedInputStreamMultipleMultiThread(10, 65536, false, true);
    }

    @Test
    public void testMultiplePipedInputStreamMultiRead() {
        try {
            for (int i = 0; i < 1002; i++) {
                testMultiplePipedInputStreamMultipleMultiThread(1, 1024, true, false);
            }
        } catch (OutOfMemoryError e) {
            System.gc();
            LOGGER.error(e);
        }
    }

    @Test
    public void testMultiplePipedInputStreamMultipleMultiThreadWithVariableSizes() {
        for (int len = 100; len < 2200; len += 500) {
            testMultiplePipedInputStreamMultipleMultiThread(1, len, true, false);
            testMultiplePipedInputStreamMultipleMultiThread(10, len, true, false);
    
            testMultiplePipedInputStreamMultipleMultiThread(1, len, false, false);
            testMultiplePipedInputStreamMultipleMultiThread(10, len, false, false);
        }
        for (int len = 100; len < 80000; len += 10000) {
            testMultiplePipedInputStreamMultipleMultiThread(1, len, true, false);
            testMultiplePipedInputStreamMultipleMultiThread(10, len, true, false);

            testMultiplePipedInputStreamMultipleMultiThread(1, len, false, false);
            testMultiplePipedInputStreamMultipleMultiThread(10, len, false, false);
        }
    }

    @Test
    public void testClose() {
        final int size = 8192;
        final int nb = 10;
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, nb)) {
            final InputStream[] is = new InputStream[nb];
            for (int i = 0; i < nb; i++) {
                is[i] = mish.getInputStream(i);
            }
            final byte[] buffer = new byte[size];
            int rank = 1;
            for (int i = 0; i < 100; i++) {
                rank = (rank + 1) % nb;
                if (is[rank].read(buffer) < 0) {
                    break;
                }
            }
            mish.hasException();
            mish.close();
            for (int i = 0; i < nb; i++) {
                assertEquals("rank: " + i, 0, is[i].available());
            }
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleClose() {
        final int size = 8192;
        final int nb = 10;
        try (FakeInputStream fakeInputStream = new FakeInputStream(INPUTSTREAM_SIZE);
                MultiplePipedInputStream mish = new MultiplePipedInputStream(fakeInputStream, nb)) {
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
                assertEquals("rank: " + i, 0, is[i].available());
            }
            mish.hasException();
            mish.close();
        } catch (final IOException e) {
            fail("Should not raized an exception: " + e.getMessage());
        }
    }

    @Test
    public void testConcurrentMultipleIntputStreamHandler() {
        int old = VitamConfiguration.getDelayMultipleInputstream();
        VitamConfiguration.setDelayMultipleInputstream(2000);
        
        List<FakeInputStream> listStream = new ArrayList<>(VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler() + 1);
        try {
            List<MultiplePipedInputStream> list = new ArrayList<>(VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler());
            for (int i = 0; i < VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler() + 1; i++) {
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE));
            }
            for (int i = 0; i < VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler(); i++) {
                try {
                    MultiplePipedInputStream mish = new MultiplePipedInputStream(listStream.get(i), 1);
                    list.add(mish);
                    LOGGER.debug(mish.toString());
                } catch (IllegalArgumentException e) {
                    LOGGER.error(e);
                    fail("Should not be interrupted");
                } catch (IOException e) {
                    LOGGER.error(e);
                    fail("Should not have an exception");
                }
            }
            // Try to allocate once and possible
            try {
                list.add(new MultiplePipedInputStream(listStream.get(VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler()), 1));
            } catch (IllegalArgumentException e) {
                fail("Should be interrupted");
            } catch (IOException e) {
                LOGGER.error(e);
                fail("Should not have an exception");
            }
            // Now free half of the list
            for (int i = VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler() - 1; i >= 500; i--) {
                MultiplePipedInputStream mish = list.remove(i);
                try {
                    mish.hasException();
                } catch (IOException e) {
                    LOGGER.error(e);
                    fail("Should not have an exception");
                }
                mish.close();
                LOGGER.debug(mish.toString());
                StreamUtils.closeSilently(listStream.remove(i));
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE));
            }
            // Now reallocate 500
            for (int i = 500; i < VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler(); i++) {
                try {
                    MultiplePipedInputStream mish = new MultiplePipedInputStream(listStream.get(i), 1);
                    list.add(mish);
                    LOGGER.debug(mish.toString());
                } catch (IllegalArgumentException | IOException e) {
                    LOGGER.error(e);
                    fail("Should not be interrupted");
                }
            }
            for (MultiplePipedInputStream mish : list) {
                try {
                    mish.hasException();
                } catch (IOException e) {
                    LOGGER.error(e);
                    fail("Should not have an exception");
                }
                mish.close();
                LOGGER.debug(mish.toString());
            }
            for (FakeInputStream fakeInputStream : listStream) {
                StreamUtils.closeSilently(fakeInputStream);
            }
            for (int i = 0; i < VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler(); i++) {
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE));
            }
            for (int i = 0; i < VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler(); i++) {
                try {
                    MultiplePipedInputStream mish = new MultiplePipedInputStream(listStream.get(i), 1);
                    LOGGER.debug(mish.toString());
                    InputStream stream = mish.getInputStream(0);
                    StreamUtils.closeSilently(stream);
                    mish.hasException();
                    mish.close();
                    LOGGER.debug(mish.toString());
                } catch (IllegalArgumentException | IOException e) {
                    LOGGER.error(e);
                    fail("Should not be interrupted");
                }
            }
        } finally {
            for (FakeInputStream fakeInputStream : listStream) {
                StreamUtils.closeSilently(fakeInputStream);
            }
            VitamConfiguration.setDelayMultipleInputstream(old);
        }
    }
    @Test
    public void testConcurrentMultipleThreadIntputStreamHandler() {
        int old = VitamConfiguration.getDelayMultipleInputstream();
        VitamConfiguration.setDelayMultipleInputstream( 2000);
        
        int nb = VitamConfiguration.getMaxConcurrentMultipleInputstreamHandler();
        List<FakeInputStream> listStream = new ArrayList<>(nb + 1);
        try {
            List<MultiplePipedInputStream> list = new ArrayList<>(nb);
            for (int i = 0; i < nb; i++) {
                listStream.add(new FakeInputStream(INPUTSTREAM_SIZE));
            }
            final ExecutorService executor = Executors.newFixedThreadPool(nb);
            list.clear();
            @SuppressWarnings("unchecked")
            final Future<Integer>[] total = new Future[nb];
            for (int i = 0; i < nb; i++) {
                MultiplePipedInputStream mish = new MultiplePipedInputStream(listStream.get(i), 1);
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
            for (MultiplePipedInputStream mish : list) {
                mish.hasException();
                mish.close();
            }

        } catch (IOException e) {
            LOGGER.error(e);
            fail("Should not have an exception");
        } finally {
            for (FakeInputStream fakeInputStream : listStream) {
                StreamUtils.closeSilently(fakeInputStream);
            }
            VitamConfiguration.setDelayMultipleInputstream(old);
        }
    }

}
