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
package fr.gouv.vitam.metadata.client;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetaDataClientFactoryTest {

    @Before
    public void initFileConfiguration() {
        MetaDataClientFactory.changeMode(MetaDataClientFactory.changeConfigurationFile("metadata-client-test.conf"));
    }

    @Test
    public void getClientInstanceTest() {
        try {
            MetaDataClientFactory.changeMode(new ClientConfigurationImpl(null, 10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", -10));
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        MetaDataClientFactory.changeMode(null);

        final MetaDataClient client = MetaDataClientFactory.getInstance().getClient();
        assertNotNull(client);

        final MetaDataClient client2 = MetaDataClientFactory.getInstance().getClient();
        assertNotNull(client2);

        assertNotSame(client, client2);
    }

    @Test
    public void changeDefautlClientTypeTest() {
        final MetaDataClient client = MetaDataClientFactory.getInstance().getClient();
        assertTrue(client instanceof MetaDataClientRest);
        assertEquals(
            VitamClientFactoryInterface.VitamClientType.PRODUCTION,
            MetaDataClientFactory.getInstance().getVitamClientType()
        );

        MetaDataClientFactory.changeMode(null);
        final MetaDataClient client2 = MetaDataClientFactory.getInstance().getClient();
        // assertTrue(client instanceof MetaDataClientMock); actually only on implementation exists
        assertTrue(client2 instanceof MetaDataClientMock);
        assertEquals(
            VitamClientFactoryInterface.VitamClientType.MOCK,
            MetaDataClientFactory.getInstance().getVitamClientType()
        );

        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("server", 1025));
        final MetaDataClient client3 = MetaDataClientFactory.getInstance().getClient();
        assertTrue(client3 instanceof MetaDataClientRest);
        assertEquals(
            VitamClientFactoryInterface.VitamClientType.PRODUCTION,
            MetaDataClientFactory.getInstance().getVitamClientType()
        );
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        MetaDataClientFactory.changeMode(MetaDataClientFactory.changeConfigurationFile("tmp"));
        final MetaDataClient client = MetaDataClientFactory.getInstance().getClient();
        // assertTrue(client instanceof MetaDataClientMock); actually only one implementation exists
        assertTrue(client instanceof MetaDataClientMock);
        assertEquals(
            VitamClientFactoryInterface.VitamClientType.MOCK,
            MetaDataClientFactory.getInstance().getVitamClientType()
        );
    }

    @Test
    public void testInitWithConfigurationFile() {
        final MetaDataClient client = MetaDataClientFactory.getInstance().getClient();
        assertTrue(client instanceof MetaDataClientRest);
        assertEquals(
            VitamClientFactoryInterface.VitamClientType.PRODUCTION,
            MetaDataClientFactory.getInstance().getVitamClientType()
        );
    }
}
