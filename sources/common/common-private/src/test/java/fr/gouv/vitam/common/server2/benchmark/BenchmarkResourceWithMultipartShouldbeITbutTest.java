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
package fr.gouv.vitam.common.server2.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;

public class BenchmarkResourceWithMultipartShouldbeITbutTest {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(BenchmarkResourceWithMultipartShouldbeITbutTest.class);

    private static final int START_SIZE = 100;
    private static final long MAX_SIZE_CHECKED = 100000L;

    private static final String BENCHMARK_CONF = "benchmark-test.conf";
    private static BenchmarkApplication application;
    private static int serverPort = 8889;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MinimalTestVitamApplicationFactory<BenchmarkApplication> testFactory =
            new MinimalTestVitamApplicationFactory<BenchmarkApplication>() {

                @Override
                public StartApplicationResponse<BenchmarkApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    BenchmarkApplication.setAllowMultipart(true);
                    final BenchmarkApplication application = new BenchmarkApplication(BENCHMARK_CONF);
                    final StartApplicationResponse<BenchmarkApplication> response = startAndReturn(application);
                    BenchmarkApplication.setAllowMultipart(false);
                    return response;
                }

            };
        final StartApplicationResponse<BenchmarkApplication> response =
            testFactory.findAvailablePortSetToApplication();
        serverPort = response.getServerPort();
        application = response.getApplication();
        BenchmarkClientFactory.setConfiguration(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        JunitHelper.getInstance().releasePort(serverPort);
    }

    private static void checkSizeLimit(BenchmarkClientRest client, String method, long size, List<String> list) {
        final long start = System.nanoTime();
        final long receivedSize = client.upload(method, size);
        if (receivedSize != size) {
            LOGGER.error(method + ":" + size + " = " + receivedSize);
        }
        assertEquals(size, receivedSize);
        final long stop = System.nanoTime();
        list.add(stop - start + " (" + (stop - start) / size + ")");
        LOGGER.info("Size: " + size + " Time: " + (stop - start) / size + " ns/bytes");
    }

    private static void printFinalResul(List<List<String>> globalTests) {
        final StringBuilder builder = new StringBuilder("Result\n");
        for (final List<String> list : globalTests) {
            for (final String info : list) {
                builder.append(" ; ").append(info);
            }
            builder.append("\n");
        }
        LOGGER.warn(builder.toString());
    }

    @Test
    public final void testMultipart() {
        final List<List<String>> globalTests = new ArrayList<>();
        List<String> testList = new ArrayList<>();
        testList.add("CONNECTOR");
        testList.add("StatusGET");
        testList.add("MultipartPOST" + MAX_SIZE_CHECKED);
        testList.add("MEMORY_USED");
        globalTests.add(testList);
        for (final BenchmarkConnectorProvider mode : BenchmarkConnectorProvider.values()) {
            if (mode == BenchmarkConnectorProvider.APACHE_NOCHECK) {
                continue;
            }
            testList = new ArrayList<>();
            testList.add(mode.name());
            JunitHelper.awaitFullGc();
            final long available = Runtime.getRuntime().freeMemory();
            BenchmarkClientFactory.getInstanceMultipart().mode(mode);
            LOGGER.warn("START " + mode.name());

            try (final BenchmarkClientRest client =
                BenchmarkClientFactory.getInstanceMultipart().getClient()) {
                long start = System.nanoTime();
                client.checkStatus();
                long stop = System.nanoTime();
                testList.add("" + (stop - start));
                start = System.nanoTime();
                final long size = MAX_SIZE_CHECKED;
                final long receivedSize = client.multipart("fake-name.txt", size);
                stop = System.nanoTime();
                if (receivedSize != size) {
                    LOGGER.error(HttpMethod.POST + ":" + size + " = " + receivedSize);
                }
                switch (mode) {
                    case APACHE:
                    case APACHE_NOCHECK:
                    case GRIZZLY:
                    case STANDARD:
                        // Must not failed
                        assertEquals(size, receivedSize);
                        testList.add("" + (stop - start) + " (" + (stop - start) / size + ")");
                        break;
                    case NETTY:
                        // Must failed
                        assertEquals(-1, receivedSize);
                        testList.add("" + (stop - start) + ":ERROR");
                        break;
                    default:
                        break;
                }
            } catch (final Exception e1) {
                LOGGER.error(e1);
                testList.add("" + -2L);
            }

            final long availableEnd = Runtime.getRuntime().freeMemory();
            final long used = available - availableEnd;
            testList.add("" + used);
            globalTests.add(testList);
        }
        printFinalResul(globalTests);
    }

    @Test
    public final void testVariousConnector() {
        final List<List<String>> globalTests = new ArrayList<>();
        List<String> testList = new ArrayList<>();
        testList.add("CONNECTOR");
        testList.add("Status" + HttpMethod.GET);
        testList.add("Status" + HttpMethod.HEAD);
        testList.add("Status" + HttpMethod.OPTIONS);
        long size = START_SIZE;
        testList.add(HttpMethod.POST + size);
        testList.add(HttpMethod.GET + size);
        testList.add(HttpMethod.DELETE + size);
        testList.add(HttpMethod.PUT + size);
        size = MAX_SIZE_CHECKED;
        testList.add(HttpMethod.POST + size);
        testList.add(HttpMethod.GET + size);
        testList.add(HttpMethod.DELETE + size);
        testList.add(HttpMethod.PUT + size);
        testList.add("MEMORY_USED");
        globalTests.add(testList);
        for (final BenchmarkConnectorProvider mode : BenchmarkConnectorProvider.values()) {
            if (mode == BenchmarkConnectorProvider.STANDARD || mode == BenchmarkConnectorProvider.APACHE_NOCHECK) {
                continue;
            }
            testList = new ArrayList<>();
            testList.add(mode.name());
            JunitHelper.awaitFullGc();
            final long available = Runtime.getRuntime().freeMemory();
            BenchmarkClientFactory.getInstanceMultipart().mode(mode);
            LOGGER.warn("START " + mode.name());
            testBenchmark(globalTests, testList);
            final long availableEnd = Runtime.getRuntime().freeMemory();
            final long used = available - availableEnd;
            testList.add("" + used);
            globalTests.add(testList);
        }
        printFinalResul(globalTests);
    }

    public static final void testBenchmark(List<List<String>> globalTests, List<String> list) {
        try (final BenchmarkClientRest client =
            BenchmarkClientFactory.getInstanceMultipart().getClient()) {
            long start = System.nanoTime();
            client.checkStatus();
            long stop = System.nanoTime();
            list.add("" + (stop - start));
            start = System.nanoTime();
            assertTrue(client.getStatus(HttpMethod.HEAD));
            stop = System.nanoTime();
            list.add("" + (stop - start));
            start = System.nanoTime();
            assertTrue(client.getStatus(HttpMethod.OPTIONS));
            stop = System.nanoTime();
            list.add("" + (stop - start));
            long size = START_SIZE;
            checkSizeLimit(client, HttpMethod.POST, size, list);
            if (BenchmarkClientFactory.getInstanceMultipart().getMode() != BenchmarkConnectorProvider.STANDARD) {
                checkSizeLimit(client, HttpMethod.GET, size, list);
            } else {
                list.add("" + -1L);
            }
            checkSizeLimit(client, HttpMethod.DELETE, size, list);
            checkSizeLimit(client, HttpMethod.PUT, size, list);
            size = MAX_SIZE_CHECKED;
            checkSizeLimit(client, HttpMethod.POST, size, list);
            if (BenchmarkClientFactory.getInstanceMultipart().getMode() != BenchmarkConnectorProvider.STANDARD) {
                checkSizeLimit(client, HttpMethod.GET, size, list);
            } else {
                list.add("" + -1L);
            }
            checkSizeLimit(client, HttpMethod.DELETE, size, list);
            checkSizeLimit(client, HttpMethod.PUT, size, list);
        } catch (final VitamClientException | VitamApplicationServerException e1) {
            list.add("" + -2L);
            globalTests.add(list);
            printFinalResul(globalTests);
            fail("Cannot connect to server");
        }
    }
}
