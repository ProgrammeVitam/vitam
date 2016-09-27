/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.common.format.identification.siegfried;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for client (and parameters) factory
 */
public class SiegfriedClientFactoryTest {

    @Before
    public void initFileConfiguration() {
        SiegfriedClientFactory.getInstance().changeConfiguration("localhost", 5138);
    }

    @Test
    public void getClientInstanceTest() {
        try {
            SiegfriedClientFactory.setConfiguration(SiegfriedClientFactory.SiegfriedClientType.NORMAL,
                null, 10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            SiegfriedClientFactory.setConfiguration(SiegfriedClientFactory.SiegfriedClientType.NORMAL,
                "localhost", -10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            SiegfriedClientFactory.setConfiguration(null, null, 10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        SiegfriedClientFactory
            .setConfiguration(SiegfriedClientFactory.SiegfriedClientType.MOCK, null, -1);

        final SiegfriedClient client =
            SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertNotNull(client);

        final SiegfriedClient client2 =
            SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertNotNull(client2);

        assertNotSame(client, client2);
    }

    @Test
    public void changeDefaultClientTypeTest() {
        final SiegfriedClient client =
            SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertTrue(client instanceof SiegfriedClientRest);
        final SiegfriedClientFactory.SiegfriedClientType type =
            SiegfriedClientFactory.getDefaultSiegfriedClientType();
        assertNotNull(type);
        assertEquals(SiegfriedClientFactory.SiegfriedClientType.NORMAL, type);

        SiegfriedClientFactory
            .setConfiguration(SiegfriedClientFactory.SiegfriedClientType.MOCK, "", 0);
        final SiegfriedClient client2 =
            SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertTrue(client2 instanceof SiegfriedClientMock);
        final SiegfriedClientFactory.SiegfriedClientType type2 =
            SiegfriedClientFactory.getDefaultSiegfriedClientType();
        assertNotNull(type2);
        assertEquals(SiegfriedClientFactory.SiegfriedClientType.MOCK, type2);

        SiegfriedClientFactory.setConfiguration(SiegfriedClientFactory.SiegfriedClientType.NORMAL,
            "server", 1025);
        final SiegfriedClient client3 =
            SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertTrue(client3 instanceof SiegfriedClientRest);
        final SiegfriedClientFactory.SiegfriedClientType type3 =
            SiegfriedClientFactory.getDefaultSiegfriedClientType();
        assertNotNull(type3);
        assertEquals(SiegfriedClientFactory.SiegfriedClientType.NORMAL, type3);
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        SiegfriedClientFactory.getInstance().changeConfiguration(null, 0);
        final SiegfriedClient client = SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertTrue(client instanceof SiegfriedClientMock);
        final SiegfriedClientFactory.SiegfriedClientType type =
            SiegfriedClientFactory.getDefaultSiegfriedClientType();
        assertNotNull(type);
        assertEquals(SiegfriedClientFactory.SiegfriedClientType.MOCK, type);
    }

    @Test
    public void testInitWithConfigurationFile() {
        final SiegfriedClient client =
            SiegfriedClientFactory.getInstance().getSiegfriedClient();
        assertTrue(client instanceof SiegfriedClientRest);
        final SiegfriedClientFactory.SiegfriedClientType type =
            SiegfriedClientFactory.getDefaultSiegfriedClientType();
        assertNotNull(type);
        assertEquals(SiegfriedClientFactory.SiegfriedClientType.NORMAL, type);
    }

}
