package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.external.client.configuration.ClientConfigurationImpl;

public class AccessExternalClientFactoryTest {
    @Before
    public void initFileConfiguration() {
        AccessExternalClientFactory
            .changeMode(AccessExternalClientFactory.changeConfigurationFile("access-external-client-test.conf"));

    }

    @Test
    public void getClientInstanceTest() {
        try {
            AccessExternalClientFactory.changeMode(new ClientConfigurationImpl(null, 10));;
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }

        try {
            AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        try {
            AccessExternalClientFactory.changeMode(new ClientConfigurationImpl());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }

        AccessExternalClientFactory.changeMode((SecureClientConfiguration)null);

        final AccessExternalClient client =
            AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final AccessExternalClient client2 =
            AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client2);
        assertNotSame(client, client2);

        AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AccessExternalClient client3 =
            AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AccessExternalClientRest);

    }

    @Test
    public void changeDefaultClientTypeTest() {
        final AccessExternalClient client =
            AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AccessExternalClientFactory.getInstance().getVitamClientType());

        AccessExternalClientFactory.changeMode((SecureClientConfiguration)null);
        final AccessExternalClient client2 =
            AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client2 instanceof AccessExternalClientMock);
        assertEquals(VitamClientType.MOCK, AccessExternalClientFactory.getInstance().getVitamClientType());

        AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AccessExternalClient client3 =
            AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AccessExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AccessExternalClientFactory.getInstance().getVitamClientType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeClientTypeAndGetExceptionTest() {
        AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("localhost", 100));
        AccessExternalClientFactory.getInstance().setVitamClientType(VitamClientType.valueOf("BAD"));
        AccessExternalClientFactory.getInstance().getClient();
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        AccessExternalClientFactory.changeMode(
            AccessExternalClientFactory.changeConfigurationFile("tmp"));
        final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientMock);
        assertEquals(VitamClientType.MOCK, AccessExternalClientFactory.getInstance().getVitamClientType());
    }

    @Test
    public void testInitWithConfigurationFile() {
        final AccessExternalClient client =
            AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AccessExternalClientFactory.getInstance().getVitamClientType());
    }
}
