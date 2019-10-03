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

public class AdminExternalClientFactoryTest {
    @Before
    public void initFileConfiguration() {
        AdminExternalClientFactory
            .changeMode(AdminExternalClientFactory.changeConfigurationFile("access-external-client-test.conf"));

    }

    @Test
    public void getClientInstanceTest() {
        try {
            AdminExternalClientFactory.changeMode(new ClientConfigurationImpl(null, 10));;
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }

        try {
            AdminExternalClientFactory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        try {
            AdminExternalClientFactory.changeMode(new ClientConfigurationImpl());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }

        AdminExternalClientFactory.changeMode((SecureClientConfiguration)null);

        final AdminExternalClient client =
            AdminExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final AdminExternalClient client2 =
            AdminExternalClientFactory.getInstance().getClient();
        assertNotNull(client2);
        assertNotSame(client, client2);

        AdminExternalClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AdminExternalClient client3 =
            AdminExternalClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AdminExternalClientRest);

    }

    @Test
    public void changeDefaultClientTypeTest() {
        final AdminExternalClient client =
            AdminExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AdminExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AdminExternalClientFactory.getInstance().getVitamClientType());

        AdminExternalClientFactory.changeMode((SecureClientConfiguration)null);
        final AdminExternalClient client2 =
            AdminExternalClientFactory.getInstance().getClient();
        assertTrue(client2 instanceof AdminExternalClientMock);
        assertEquals(VitamClientType.MOCK, AdminExternalClientFactory.getInstance().getVitamClientType());

        AdminExternalClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AdminExternalClient client3 =
            AdminExternalClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AdminExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AdminExternalClientFactory.getInstance().getVitamClientType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeClientTypeAndGetExceptionTest() {
        AdminExternalClientFactory.changeMode(new ClientConfigurationImpl("localhost", 100));
        AdminExternalClientFactory.getInstance().setVitamClientType(VitamClientType.valueOf("BAD"));
        AdminExternalClientFactory.getInstance().getClient();
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        AdminExternalClientFactory.changeMode(
            AdminExternalClientFactory.changeConfigurationFile("tmp"));
        final AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AdminExternalClientMock);
        assertEquals(VitamClientType.MOCK, AdminExternalClientFactory.getInstance().getVitamClientType());
    }

    @Test
    public void testInitWithConfigurationFile() {
        final AdminExternalClient client =
            AdminExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AdminExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AdminExternalClientFactory.getInstance().getVitamClientType());
    }
}
