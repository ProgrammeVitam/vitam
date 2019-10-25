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
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BenchmarkResourceInputStreamIT extends ResteasyTestApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkResourceInputStreamIT.class);

    static BenchmarkClientFactory factory = BenchmarkClientFactory.getInstance();
    static VitamServerTestRunner
        vitamServerTestRunner = new VitamServerTestRunner(BenchmarkResourceInputStreamIT.class, factory);


    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new BenchmarkResourceProduceInputStream());
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

    @Test
    public final void testStatus() {
        try (final BenchmarkClientRest client =
            BenchmarkClientFactory.getInstance().getClient()) {
            client.checkStatus();
        } catch (final VitamApplicationServerException e) {
            fail("Cannot connect to server");
        }
    }

    @Test
    public void testStream() throws VitamClientInternalException, IOException {
        try (final BenchmarkClientRest client =
            BenchmarkClientFactory.getInstance().getClient()) {
            String method = HttpMethod.GET;
            long start = System.nanoTime();
            Response response =
                client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_ASYNC + method,
                    null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            long stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.POST;
            start = System.nanoTime();
            response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_ASYNC + method,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.PUT;
            start = System.nanoTime();
            response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_ASYNC + method,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");
        }
    }


    @Test
    public void testIndirectStream() throws VitamClientInternalException, IOException {
        try (final BenchmarkClientRest client =
            BenchmarkClientFactory.getInstance().getClient()) {
            String method = HttpMethod.GET;
            long start = System.nanoTime();
            Response response =
                client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_INDIRECT_ASYNC + method,
                    null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            long stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.POST;
            start = System.nanoTime();
            response =
                client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_INDIRECT_ASYNC + method,
                    null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.PUT;
            start = System.nanoTime();
            response =
                client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_INDIRECT_ASYNC + method,
                    null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");
        }
    }

    @Test
    public void testStreamNotAsync() throws VitamClientInternalException, IOException {
        try (final BenchmarkClientRest client = BenchmarkClientFactory.getInstance().getClient()) {
            String method = HttpMethod.GET;
            long start = System.nanoTime();
            Response response =
                client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_DIRECT + method,
                    null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =response.readEntity(InputStream.class)) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            long stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.POST;
            start = System.nanoTime();
            response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_DIRECT + method,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.PUT;
            start = System.nanoTime();
            response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_DIRECT + method,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");
        }
    }


    @Test
    public void testIndirectStreamNotAsync() throws VitamClientInternalException, IOException {
        try (final BenchmarkClientRest client =
            BenchmarkClientFactory.getInstance().getClient()) {
            String method = HttpMethod.GET;
            long start = System.nanoTime();
            Response response =
                client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_INDIRECT + method,
                    null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            long stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.POST;
            start = System.nanoTime();
            response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_INDIRECT + method,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");

            method = HttpMethod.PUT;
            start = System.nanoTime();
            response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD_INDIRECT + method,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            try (final InputStream inputStream =
                StreamUtils.getRemainingReadOnCloseInputStream(response.readEntity(InputStream.class))) {
                assertEquals(BenchmarkResourceProduceInputStream.size, JunitHelper.consumeInputStream(inputStream));
            }
            client.consumeAnyEntityAndClose(response);
            stop = System.nanoTime();
            LOGGER.warn(method + " Download " + BenchmarkResourceProduceInputStream.size + " in " +
                (stop - start) / 1000000 + "ms so " +
                (stop - start) / BenchmarkResourceProduceInputStream.size + " ns/bytes");
        }
    }
}
