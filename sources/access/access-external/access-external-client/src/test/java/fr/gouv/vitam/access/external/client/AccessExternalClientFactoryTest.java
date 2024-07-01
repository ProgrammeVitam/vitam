/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.external.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AccessExternalClientFactoryTest {

    @Before
    public void initFileConfiguration() {
        AccessExternalClientFactory.changeMode(
            AccessExternalClientFactory.changeConfigurationFile("access-external-client-test.conf")
        );
    }

    @Test
    public void getClientInstanceTest() {
        try {
            AccessExternalClientFactory.changeMode(new ClientConfigurationImpl(null, 10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {}

        try {
            AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {}
        try {
            AccessExternalClientFactory.changeMode(new ClientConfigurationImpl());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {}

        AccessExternalClientFactory.changeMode((SecureClientConfiguration) null);

        final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final AccessExternalClient client2 = AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client2);
        assertNotSame(client, client2);

        AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AccessExternalClient client3 = AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AccessExternalClientRest);
    }

    @Test
    public void changeDefaultClientTypeTest() {
        final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AccessExternalClientFactory.getInstance().getVitamClientType());

        AccessExternalClientFactory.changeMode((SecureClientConfiguration) null);
        final AccessExternalClient client2 = AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client2 instanceof AccessExternalClientMock);
        assertEquals(VitamClientType.MOCK, AccessExternalClientFactory.getInstance().getVitamClientType());

        AccessExternalClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AccessExternalClient client3 = AccessExternalClientFactory.getInstance().getClient();
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
        AccessExternalClientFactory.changeMode(AccessExternalClientFactory.changeConfigurationFile("tmp"));
        final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientMock);
        assertEquals(VitamClientType.MOCK, AccessExternalClientFactory.getInstance().getVitamClientType());
    }

    @Test
    public void testInitWithConfigurationFile() {
        final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof AccessExternalClientRest);
        assertEquals(VitamClientType.PRODUCTION, AccessExternalClientFactory.getInstance().getVitamClientType());
    }
}
