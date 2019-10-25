/*
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
package fr.gouv.vitam.common.server.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.HttpMethod;

import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

public class BenchmarkResourceManual {
    private static final int SLEEP_STARTUP = 5000;

    private static final int SLEEP_INTERMEDIARY = 100;

    private static final int START_SIZE = SLEEP_INTERMEDIARY;

    private static final int FACTOR_STEP = 10;

    private static final long MAX_SIZE = 100000000L;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkResourceManual.class);

    private static int serverPort = 8889;

    public static void main(String[] args) {
        BenchmarkClientFactory.setConfiguration(serverPort);
        final BenchmarkResourceManual test = new BenchmarkResourceManual();
        test.testVariousConnector();
        test.testThreadedMode();
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        BenchmarkClientFactory.setConfiguration(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
    }

    private static void checkSizeLimit(BenchmarkClientRest client, String method, long size, List<Long> list) {
        final long start = System.nanoTime();
        final long receivedSize = client.upload(method, size);
        if (receivedSize != size) {
            LOGGER.error(method + ":" + size + " = " + receivedSize);
        }
        assertEquals(size, receivedSize);
        final long stop = System.nanoTime();
        list.add((stop - start) / size);
        LOGGER.info("Size: " + size + " Time: " + (stop - start) / size + " ns/bytes");
    }

    private static void printFinalResul(List<List<Long>> globalTests, long maxSize) {
        final StringBuilder builder = new StringBuilder("CONNECTOR ; ");
        long size = START_SIZE;
        builder.append("Status").append(HttpMethod.GET).append(" ; ");
        builder.append("Status").append(HttpMethod.HEAD).append(" ; ");
        builder.append("Status").append(HttpMethod.OPTIONS).append(" ; ");
        builder.append(HttpMethod.POST).append(size).append(" ; ");
        for (; size <= maxSize; size *= FACTOR_STEP) {
            builder.append(HttpMethod.POST).append(size).append(" ; ")
                .append(HttpMethod.GET).append(size).append(" ; ")
                .append(HttpMethod.DELETE).append(size).append(" ; ")
                .append(HttpMethod.PUT).append(size).append(" ; ");
        }
        builder.append(" MEMORY_UZED\n");
        int i = 0;
        for (final BenchmarkConnectorProvider mode : BenchmarkConnectorProvider.values()) {
            if (mode == BenchmarkConnectorProvider.STANDARD || mode == BenchmarkConnectorProvider.APACHE_NOCHECK) {
                continue;
            }
            builder.append(mode.name());
            final List<Long> list = globalTests.get(i);
            for (final Long info : list) {
                builder.append(" ; ").append(info);
            }
            builder.append("\n");
            i++;
        }
        LOGGER.warn(builder.toString());
    }

    public final void testVariousConnector() {
        final List<List<Long>> globalTests = new ArrayList<>();
        for (final BenchmarkConnectorProvider mode : BenchmarkConnectorProvider.values()) {
            if (mode == BenchmarkConnectorProvider.STANDARD || mode == BenchmarkConnectorProvider.APACHE_NOCHECK) {
                continue;
            }
            final List<Long> testList = new ArrayList<>();
            JunitHelper.awaitFullGc();
            try {
                Thread.sleep(10000);
            } catch (final InterruptedException e) {}
            final long available = Runtime.getRuntime().freeMemory();
            LOGGER.warn("START " + mode.name());
            testBenchmark(globalTests, testList);
            final long availableEnd = Runtime.getRuntime().freeMemory();
            final long used = available - availableEnd;
            testList.add(used);
            globalTests.add(testList);
        }
        printFinalResul(globalTests, MAX_SIZE);
    }

    public final void testBenchmark(List<List<Long>> globalTests, List<Long> list) {
        try (final BenchmarkClientRest client =
            BenchmarkClientFactory.getInstance().getClient()) {
            long start = System.nanoTime();
            client.checkStatus();
            long stop = System.nanoTime();
            list.add(stop - start);
            start = System.nanoTime();
            assertTrue(client.getStatus(HttpMethod.HEAD));
            stop = System.nanoTime();
            list.add(stop - start);
            start = System.nanoTime();
            assertTrue(client.getStatus(HttpMethod.OPTIONS));
            stop = System.nanoTime();
            list.add(stop - start);
            long size = START_SIZE;
            checkSizeLimit(client, HttpMethod.POST, size, list);
            try {
                Thread.sleep(SLEEP_STARTUP);
            } catch (final InterruptedException e) {}
            for (; size <= MAX_SIZE; size *= FACTOR_STEP) {
                checkSizeLimit(client, HttpMethod.POST, size, list);
                try {
                    Thread.sleep(SLEEP_INTERMEDIARY);
                } catch (final InterruptedException e) {}
                checkSizeLimit(client, HttpMethod.GET, size, list);
                try {
                    Thread.sleep(SLEEP_INTERMEDIARY);
                } catch (final InterruptedException e) {}
                checkSizeLimit(client, HttpMethod.DELETE, size, list);
                try {
                    Thread.sleep(SLEEP_INTERMEDIARY);
                } catch (final InterruptedException e) {}
                checkSizeLimit(client, HttpMethod.PUT, size, list);
                try {
                    Thread.sleep(SLEEP_INTERMEDIARY);
                } catch (final InterruptedException e) {}
            }
        } catch (final VitamApplicationServerException | VitamClientException e1) {
            list.add(-2L);
            globalTests.add(list);
            printFinalResul(globalTests, MAX_SIZE);
            fail("Cannot connect to server");
        }
    }

    public final void testThreadedMode() {
        final List<List<Long>> globalTests = new ArrayList<>();
        for (final BenchmarkConnectorProvider mode : BenchmarkConnectorProvider.values()) {
            if (mode == BenchmarkConnectorProvider.STANDARD || mode == BenchmarkConnectorProvider.APACHE_NOCHECK) {
                continue;
            }
            final List<Long> testList = new ArrayList<>();
            JunitHelper.awaitFullGc();
            try {
                Thread.sleep(10000);
            } catch (final InterruptedException e) {}
            final ExecutorService executorService = Executors.newCachedThreadPool(VitamThreadFactory.getInstance());
            final long available = Runtime.getRuntime().freeMemory();
            LOGGER.warn("START " + mode.name());
            final long start = System.nanoTime();
            for (int i = 0; i < FACTOR_STEP; i++) {
                final BenchmarkThread thread = new BenchmarkThread();
                thread.globalTests = globalTests;
                thread.testList = testList;
                executorService.execute(thread);
            }
            executorService.shutdown();
            try {
                while (!executorService.awaitTermination(SLEEP_STARTUP, TimeUnit.MILLISECONDS)) {

                }
            } catch (final InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            final long stop = System.nanoTime();
            testList.add(stop - start);
            final long availableEnd = Runtime.getRuntime().freeMemory();
            final long used = available - availableEnd;
            testList.add(used);
            globalTests.add(testList);
        }
        printFinalResul(globalTests, MAX_SIZE);
    }

    private static class BenchmarkThread implements Runnable {
        List<List<Long>> globalTests;
        List<Long> testList;

        @Override
        public void run() {
            try (final BenchmarkClientRest client =
                BenchmarkClientFactory.getInstance().getClient()) {
                client.checkStatus();
                checkSizeLimit(client, HttpMethod.POST, MAX_SIZE / 10, testList);
            } catch (final VitamApplicationServerException e1) {
                testList.add(-2L);
                globalTests.add(testList);
                printFinalResul(globalTests, MAX_SIZE / 100);
                fail("Cannot connect to server");
            }
        }
    }
}
