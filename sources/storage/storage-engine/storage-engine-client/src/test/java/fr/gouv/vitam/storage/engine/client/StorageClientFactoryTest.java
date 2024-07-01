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
package fr.gouv.vitam.storage.engine.client;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class StorageClientFactoryTest {

    private final ClientConfiguration configuration = new ClientConfigurationImpl();

    @Before
    public void initFileConfiguration() {
        StorageClientFactory.changeMode(StorageClientFactory.changeConfigurationFile("storage-client-test.conf"));
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        StorageClientFactory.changeMode((ClientConfiguration) null);
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertTrue(client instanceof StorageClientMock);
        assertEquals(VitamClientType.MOCK, StorageClientFactory.getInstance().getVitamClientType());
    }

    @Test
    public void testInitWithConfigurationFile() {
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertTrue(client instanceof StorageClientRest);
        assertEquals(VitamClientType.PRODUCTION, StorageClientFactory.getInstance().getVitamClientType());
    }

    @Test
    public void getClientInstanceTest() {
        try {
            StorageClientFactory.changeMode(new ClientConfigurationImpl(null, 10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            StorageClientFactory.changeMode(configuration);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        StorageClientFactory.changeMode((ClientConfiguration) null);

        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertNotNull(client);

        final StorageClient client2 = StorageClientFactory.getInstance().getClient();
        assertNotNull(client2);

        assertNotSame(client, client2);
    }

    @Test
    public void changeDefaultClientTypeTest() {
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertTrue(client instanceof StorageClientRest);
        assertEquals(VitamClientType.PRODUCTION, StorageClientFactory.getInstance().getVitamClientType());

        StorageClientFactory.changeMode((ClientConfiguration) null);
        final StorageClient client2 = StorageClientFactory.getInstance().getClient();
        assertTrue(client2 instanceof StorageClientMock);
        assertEquals(VitamClientType.MOCK, StorageClientFactory.getInstance().getVitamClientType());

        StorageClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final StorageClient client3 = StorageClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof StorageClientRest);
        assertEquals(VitamClientType.PRODUCTION, StorageClientFactory.getInstance().getVitamClientType());
    }
}
