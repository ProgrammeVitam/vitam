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
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class VitamServerFactoryTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamServerFactory.class);
    private final String JETTY_CONFIG_TEST = "jetty-config-test.xml";

    @Test
    public final void testNewVitamServerOnDefaultPort() {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int serverPort = junitHelper.findAvailablePort();
        final int oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(serverPort);

        final VitamServer server = VitamServerFactory.newVitamServerOnDefaultPort();
        assertEquals(serverPort, server.getPort());
        assertNull(server.getHandler());
        assertFalse(server.isConfigured());
        try {
            server.configure(null);
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
        try {
            server.startAndJoin();
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
        junitHelper.releasePort(serverPort);
        VitamServerFactory.setDefaultPort(oldPort);
    }

    @Test
    public final void testNewVitamServer() {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final VitamServer server = VitamServerFactory.newVitamServer(port);
        assertEquals(port, server.getPort());
        assertNull(server.getHandler());
        assertFalse(server.isConfigured());
        try {
            server.configure(null);
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
        try {
            server.startAndJoin();
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
        junitHelper.releasePort(port);
    }

    @Test
    public final void testNewVitamServerFromJettyConfig() {
        try {
            final VitamServer server = VitamServerFactory.newVitamServerByJettyConf(JETTY_CONFIG_TEST);
            final Server jettyServer = server.getServer();

            jettyServer.start();
            Assert.assertTrue(jettyServer.isStarted());

            if (jettyServer != null && jettyServer.isStarted()) {
                jettyServer.stop();
            }
            Assert.assertTrue(jettyServer.isStopped());
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public final void testNewVitamServerFromNotJettyConfig() {
        VitamServer server = null;
        try {
            server = VitamServerFactory.newVitamServerByJettyConf(null);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
            Assert.assertTrue(server == null);
        }
    }

    @Test
    public final void testSetterGetter() {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final int oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(port);
        assertEquals(port, VitamServerFactory.getDefaultPort());
        VitamServerFactory.setDefaultPort(oldPort);
        assertEquals(oldPort, VitamServerFactory.getDefaultPort());
        try {
            VitamServerFactory.setDefaultPort(-1);
            fail("Should raized an axception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        VitamServerFactory.setDefaultPort(oldPort);
        junitHelper.releasePort(port);
    }
}
