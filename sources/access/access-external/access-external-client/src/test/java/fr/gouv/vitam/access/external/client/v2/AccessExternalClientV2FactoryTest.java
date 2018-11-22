package fr.gouv.vitam.access.external.client.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.external.client.configuration.ClientConfigurationImpl;
import org.junit.Before;
import org.junit.Test;

public class AccessExternalClientV2FactoryTest {
    @Before
    public void initFileConfiguration() {
        AccessExternalClientV2Factory
            .changeMode(AccessExternalClientV2Factory.changeConfigurationFile("access-external-client-test.conf"));

    }

    @Test
    public void getClientInstanceTest() {
        try {
            AccessExternalClientV2Factory.changeMode(new ClientConfigurationImpl(null, 10));;
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }

        try {
            AccessExternalClientV2Factory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        try {
            AccessExternalClientV2Factory.changeMode(new ClientConfigurationImpl());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }

        AccessExternalClientV2Factory.changeMode(null);

        final AccessExternalClientV2 client =
                AccessExternalClientV2Factory.getInstance().getClient();
        assertNotNull(client);

        final AccessExternalClientV2 client2 =
                AccessExternalClientV2Factory.getInstance().getClient();
        assertNotNull(client2);
        assertNotSame(client, client2);

        AccessExternalClientV2Factory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AccessExternalClientV2 client3 =
                AccessExternalClientV2Factory.getInstance().getClient();
        assertTrue(client3 instanceof AccessExternalClientV2Rest);

    }

    @Test(expected = IllegalArgumentException.class)
    public void changeClientTypeAndGetExceptionTest() {
        AccessExternalClientV2Factory.changeMode(new ClientConfigurationImpl("localhost", 100));
        AccessExternalClientV2Factory.getInstance().setVitamClientType(VitamClientType.valueOf("BAD"));
        AccessExternalClientV2Factory.getInstance().getClient();
    }

    @Test
    public void testInitWithConfigurationFile() {
        final AccessExternalClientV2 client =
            AccessExternalClientV2Factory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientV2Rest);
        assertEquals(VitamClientType.PRODUCTION, AccessExternalClientV2Factory.getInstance().getVitamClientType());
    }
}
