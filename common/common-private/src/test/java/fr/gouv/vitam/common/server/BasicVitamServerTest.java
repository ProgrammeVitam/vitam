package fr.gouv.vitam.common.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;

public class BasicVitamServerTest {

    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should raized an exception";
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static class MyRunner extends Thread {
        BasicVitamServer server;

        MyRunner(BasicVitamServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                server.run();
            } catch (final VitamApplicationServerException e) {
                System.err.println("Should not");
                // ignore
            }
        }

    }

    @Test
    public final void testBuild() {
        final BasicVitamServer server = new BasicVitamServer(8082);
        try {
            server.configure(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {}
        try {
            server.setHandler(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {}
        try {
            server.run();
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
    }

}
