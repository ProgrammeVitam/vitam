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
package fr.gouv.vitam.common.server2.application.configuration;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.apache.connector.VitamClientProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

public class DisableAutomaticRetryTest extends JerseyTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DisableAutomaticRetryTest.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AutoCloseServer server = new AutoCloseServer();

    public DisableAutomaticRetryTest() {
        super((TestContainerFactory) null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        executor.submit(server);
        Thread.yield();
        Thread.sleep(100);
        Assume.assumeTrue("Cant start server using Jersey", server.run);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (server != null && server.serverSocket != null) {
            server.serverSocket.close();
        }
        executor.shutdownNow();
        if (executor.awaitTermination(5, TimeUnit.SECONDS) == false) {
            LOGGER.error("Executor timeout on shutdown");
        }
    }

    @Test
    public void testAutomaticRetry() {
        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        final Client client = ClientBuilder.newClient(config);
        final WebTarget r = client.target(getBaseUri());
        try {
            r.request().get(String.class);
            fail("request should fail");
        } catch (final Exception e) {}
        // 1 times + retry 3 times
        Assume.assumeTrue(4 == server.connectionCount);
    }

    @Test
    public void testDisableAutomaticRetry() {
        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(VitamClientProperties.DISABLE_AUTOMATIC_RETRIES, true);
        final Client client = ClientBuilder.newClient(config);
        final WebTarget r = client.target(getBaseUri());
        try {
            r.request().get(String.class);
            fail("request should fail");
        } catch (final Exception e) {}

        Assume.assumeTrue(1 == server.connectionCount);
    }

    public class AutoCloseServer implements Runnable {
        public ServerSocket serverSocket;
        public int connectionCount = 0;
        public volatile boolean run = false;
        @Override
        public void run() {
            LOGGER.info("AutoCloseServer started");
            final int port = getPort();
            try {
                serverSocket = new ServerSocket(port);
                run = true;
                while (true) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        connectionCount++;
                        LOGGER.info("AutoCloseServer received connection");
                    }
                }
            } catch (final IOException e) {} finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (final IOException e) {}
                }
            }
        }
    }

    @Path("dummy")
    public static class DummyResource {
    }

    @Override
    protected Application configure() {
        final ResourceConfig config = new ResourceConfig(DummyResource.class);
        return config;
    }
}
