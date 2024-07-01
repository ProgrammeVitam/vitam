/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.server;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BasicVitamServerTest {

    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should raized an exception";
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";
    private static final String JETTY_CONFIG_FILE = "jetty-config-test.xml";
    private static final String JETTY_CONFIG_FILE_KO1 = "jetty-test-ko1.xml";
    private static final String JETTY_CONFIG_FILE_KO2 = "jetty-test-ko2.xml";
    private static final String JETTY_CONFIG_FILE_KO_NOTFOUND = "jetty-test-notFound.xml";

    private static class MyRunner extends Thread {

        BasicVitamServer server;

        MyRunner(BasicVitamServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                server.startAndJoin();
            } catch (final VitamApplicationServerException e) {
                System.err.println("Should not");
                // ignore
            }
        }
    }

    @Test
    public final void testBuild() {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final BasicVitamServer server = new BasicVitamServer(port);
        try {
            server.configure(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {}
        try {
            server.setHandler(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {}
        try {
            server.startAndJoin();
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {}
        try {
            server.stop();
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {}
        assertNotNull(server.getServer());
        final MyRunner myRunner = new MyRunner(server);
        assertFalse(server.isConfigured());
        server.setConfigured(true);
        assertTrue(server.isConfigured());
        myRunner.start();
        try {
            server.getServer().stop();
        } catch (final Exception e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        server.getServer().destroy();
        junitHelper.releasePort(port);
    }

    @Test
    public final void testStartingServerWithCorrectConfig() {
        try {
            final JunitHelper junitHelper = JunitHelper.getInstance();
            final int port = junitHelper.findAvailablePort();
            final BasicVitamServer server = new BasicVitamServer(JETTY_CONFIG_FILE);
            assertTrue(server.isConfigured());
            assertNotNull(server.getServerConfiguration());
            assertFalse(server.isStarted());
            assertTrue(server.isStopped());
            server.start();
            assertTrue(server.isStarted());
            assertFalse(server.isStopped());

            try {
                server.stop();
            } catch (final Exception e) {
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }
            assertTrue(server.isStopped());
            server.getServer().destroy();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            assertTrue(false);
        }
    }

    @Test
    public final void testNotStartServerWithConfigFailed() {
        try {
            final BasicVitamServer server = new BasicVitamServer(JETTY_CONFIG_FILE_KO1);
            assertTrue(server.isConfigured());

            server.start();

            try {
                server.getServer().stop();
            } catch (final Exception e) {
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }
            server.getServer().destroy();
        } catch (final VitamApplicationServerException e) {
            assertFalse(false);
        }
    }

    @Test
    public final void testNotStartServerWithConfigNotFound() {
        try {
            new BasicVitamServer(JETTY_CONFIG_FILE_KO_NOTFOUND);
        } catch (final VitamApplicationServerException e) {
            assertFalse(false);
        }
    }

    @Test
    public final void testNotStartServerWithConfigCantBeParse() {
        try {
            new BasicVitamServer(JETTY_CONFIG_FILE_KO2);
        } catch (final VitamApplicationServerException e) {
            assertFalse(false);
        }
    }
}
