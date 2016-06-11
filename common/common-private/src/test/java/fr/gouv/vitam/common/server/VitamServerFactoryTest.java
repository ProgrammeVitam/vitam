package fr.gouv.vitam.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;

public class VitamServerFactoryTest {

    @Test
    public final void testNewVitamServerOnDefaultPort() {
        final VitamServer server = VitamServerFactory.newVitamServerOnDefaultPort();
        assertEquals(8082, server.getPort());
        assertNull(server.getHandler());
        assertFalse(server.isConfigured());
        try {
            server.configure(null);
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
        try {
            server.run();
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
    }

    @Test
    public final void testNewVitamServer() {
        final VitamServer server = VitamServerFactory.newVitamServer(8081);
        assertEquals(8081, server.getPort());
        assertNull(server.getHandler());
        assertFalse(server.isConfigured());
        try {
            server.configure(null);
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
        try {
            server.run();
            fail("Should raized an axception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
    }

}
