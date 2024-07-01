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
package fr.gouv.vitam.common.client;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import org.junit.Test;

import javax.ws.rs.client.Client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class VitamClientFactoryTest {

    private static final String RESOURCE_PATH = "service/v1";
    private static final ClientConfiguration CONFIGURATION = new ClientConfigurationImpl(
        TestVitamClientFactory.LOCALHOST,
        1234
    );

    private static class ClientFactoryTest extends VitamClientFactory<BasicClient> {

        public ClientFactoryTest(ClientConfiguration configuration, String resourcePath, Client client) {
            super(configuration, resourcePath, client);
        }

        public ClientFactoryTest(ClientConfiguration configuration, String resourcePath) {
            super(configuration, resourcePath);
        }

        @Override
        public BasicClient getClient() {
            return new DefaultClient(this);
        }
    }

    @Test
    public void vitamClientFactoryTest() {
        final Client client = mock(Client.class);
        final ClientFactoryTest cft = new ClientFactoryTest(CONFIGURATION, RESOURCE_PATH, client);
        assertEquals("/" + RESOURCE_PATH, cft.getResourcePath());
        assertEquals(client, cft.getHttpClient(false));
        assertEquals(cft.getHttpClient(), cft.getHttpClient(false));
        assertTrue(cft.toString().contains(RESOURCE_PATH));
        assertEquals(VitamClientType.PRODUCTION, cft.getVitamClientType());
        try (BasicClient vitamClient = cft.getClient()) {
            assertEquals(
                ((DefaultClient) vitamClient).getClientFactory().getClientConfiguration(),
                cft.getClientConfiguration()
            );
            assertTrue(vitamClient.toString().contains(cft.toString()));
        }
    }

    @Test
    public void vitamClientMockFactoryTest() {
        final Client client = mock(Client.class);
        final ClientFactoryTest cft = new ClientFactoryTest(null, RESOURCE_PATH, client);
        assertEquals("/" + RESOURCE_PATH, cft.getResourcePath());
        assertEquals(client, cft.getHttpClient(false));
        assertEquals(cft.getHttpClient(), cft.getHttpClient(false));
        assertTrue(cft.toString().contains(RESOURCE_PATH));
        assertEquals(VitamClientType.MOCK, cft.getVitamClientType());
        try (BasicClient vitamClient = cft.getClient()) {
            assertEquals(
                ((DefaultClient) vitamClient).getClientFactory().getClientConfiguration(),
                cft.getClientConfiguration()
            );
            assertTrue(vitamClient.toString().contains(cft.toString()));
        }
    }
}
