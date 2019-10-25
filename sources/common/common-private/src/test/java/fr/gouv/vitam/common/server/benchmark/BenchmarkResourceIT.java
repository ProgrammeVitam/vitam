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

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BenchmarkResourceIT extends ResteasyTestApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkResourceIT.class);

    private static final int REPEAT_TIME = 5;
    private static final int START_SIZE = 100;
    private static final long MAX_SIZE_CHECKED = 1000000L;

    static BenchmarkClientFactory factory = BenchmarkClientFactory.getInstance();
    static VitamServerTestRunner
        vitamServerTestRunner = new VitamServerTestRunner(BenchmarkResourceIT.class, factory);


    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new BenchmarkResource());
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        LOGGER.debug("Ending tests");
        vitamServerTestRunner.runAfter();
    }

    private static void checkSizeLimit(BenchmarkClientRest client, String method, long size, List<String> list) {
        final long start = System.nanoTime();
        for (int i = 0; i < REPEAT_TIME; i++) {
            final long receivedSize = client.upload(method, size);
            if (receivedSize != size) {
                LOGGER.error(method + ":" + size + " = " + receivedSize);
            }
            assertEquals(size, receivedSize);
            final long receivedSize2 = client.download(method, size);
            if (receivedSize2 != size) {
                LOGGER.error(method + ":" + size + " = " + receivedSize2);
            }
            assertEquals(size, receivedSize2);
        }
        final long stop = System.nanoTime();
        list.add(stop - start + " (" + (stop - start) / (size * REPEAT_TIME) + ")");
        LOGGER.info("Size: " + size + " Time: " + (stop - start) / (size * REPEAT_TIME) + " ns/bytes");
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
        final BenchmarkConnectorProvider mode = BenchmarkConnectorProvider.APACHE;
        testList = new ArrayList<>();
        testList.add(mode.name());
        JunitHelper.awaitFullGc();
        final long available = Runtime.getRuntime().freeMemory();
        LOGGER.warn("START " + mode.name());
        testBenchmark(globalTests, testList);
        final long availableEnd = Runtime.getRuntime().freeMemory();
        final long used = available - availableEnd;
        testList.add("" + used);
        globalTests.add(testList);
        printFinalResul(globalTests);
    }

    public static final void testBenchmark(List<List<String>> globalTests, List<String> list) {
        for (int i = 0; i < REPEAT_TIME; i++) {
            try (final BenchmarkClientRest client =
                BenchmarkClientFactory.getInstance().getClient()) {
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
                checkSizeLimit(client, HttpMethod.GET, size, list);
                checkSizeLimit(client, HttpMethod.DELETE, size, list);
                checkSizeLimit(client, HttpMethod.PUT, size, list);
                size = MAX_SIZE_CHECKED;
                checkSizeLimit(client, HttpMethod.POST, size, list);
                checkSizeLimit(client, HttpMethod.GET, size, list);
                checkSizeLimit(client, HttpMethod.DELETE, size, list);
                checkSizeLimit(client, HttpMethod.PUT, size, list);
                list.add("\n\t");
            } catch (final VitamClientException | VitamApplicationServerException e1) {
                list.add("" + -2L);
                globalTests.add(list);
                printFinalResul(globalTests);
                fail("Cannot connect to server");
            }
        }
    }

}
