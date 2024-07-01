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
package fr.gouv.vitam.functional.administration.client;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AdminManagementClientFactoryTest {

    @Test
    public void getClientInstanceTest() {
        try {
            AdminManagementClientFactory.changeMode(new ClientConfigurationImpl(null, 10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {}

        try {
            AdminManagementClientFactory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {}
        try {
            AdminManagementClientFactory.changeMode(new ClientConfigurationImpl());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {}

        AdminManagementClientFactory.changeMode(null);

        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        assertNotNull(client);

        final AdminManagementClient client2 = AdminManagementClientFactory.getInstance().getClient();
        assertNotNull(client2);
        assertNotSame(client, client2);

        AdminManagementClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AdminManagementClient client3 = AdminManagementClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AdminManagementClientRest);
    }

    @Test
    public void changeDefaultClientTypeTest() {
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        assertTrue(client instanceof AdminManagementClientRest);
        assertEquals(VitamClientType.PRODUCTION, AdminManagementClientFactory.getInstance().getVitamClientType());

        AdminManagementClientFactory.changeMode(null);
        final AdminManagementClient client2 = AdminManagementClientFactory.getInstance().getClient();
        assertTrue(client2 instanceof AdminManagementClientMock);
        assertEquals(VitamClientType.MOCK, AdminManagementClientFactory.getInstance().getVitamClientType());

        AdminManagementClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final AdminManagementClient client3 = AdminManagementClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof AdminManagementClientRest);
        assertEquals(VitamClientType.PRODUCTION, AdminManagementClientFactory.getInstance().getVitamClientType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeClientTypeAndGetExceptionTest() {
        AdminManagementClientFactory.changeMode(new ClientConfigurationImpl("localhost", 100));
        AdminManagementClientFactory.getInstance().setVitamClientType(VitamClientType.valueOf("BAD"));
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        AdminManagementClientFactory.changeMode(AdminManagementClientFactory.changeConfigurationFile("tmp"));
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        assertTrue(client instanceof AdminManagementClientMock);
        assertEquals(VitamClientType.MOCK, AdminManagementClientFactory.getInstance().getVitamClientType());
    }

    @Test
    public void testInitWithConfigurationFile() {
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        assertTrue(client instanceof AdminManagementClientRest);
        assertEquals(VitamClientType.PRODUCTION, AdminManagementClientFactory.getInstance().getVitamClientType());
    }
}
