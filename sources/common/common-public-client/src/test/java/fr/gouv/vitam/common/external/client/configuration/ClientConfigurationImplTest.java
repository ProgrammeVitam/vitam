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
package fr.gouv.vitam.common.external.client.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ClientConfigurationImplTest {

    @Test
    public void testFailed() {
        ClientConfigurationImpl clientConfigurationImpl0 = new ClientConfigurationImpl();
        try {
            clientConfigurationImpl0.setServerPort(0);
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
        try {
            clientConfigurationImpl0.setServerPort(-1);
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
        try {
            clientConfigurationImpl0.setServerHost(null);
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
        try {
            clientConfigurationImpl0.setServerHost("");
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
        try {
            clientConfigurationImpl0 = new ClientConfigurationImpl(null, 10);
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
        try {
            clientConfigurationImpl0 = new ClientConfigurationImpl("", 10);
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
        try {
            clientConfigurationImpl0 = new ClientConfigurationImpl("test", -10);
            fail("Expecting exception: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {}
    }

    @Test
    public void testBuildOk() {
        ClientConfigurationImpl clientConfigurationImpl0 = new ClientConfigurationImpl("H.Y", 75);
        assertEquals(75, clientConfigurationImpl0.getServerPort());
        clientConfigurationImpl0 = new ClientConfigurationImpl();
        final int int0 = clientConfigurationImpl0.getServerPort();
        assertEquals(0, int0);
    }

    @Test
    public void testSetterPort() {
        final ClientConfigurationImpl clientConfigurationImpl0 = new ClientConfigurationImpl();
        final ClientConfigurationImpl clientConfigurationImpl1 = clientConfigurationImpl0.setServerPort(470);
        final int int0 = clientConfigurationImpl1.getServerPort();
        assertEquals(470, int0);
    }

    @Test
    public void testSetterHost() {
        final ClientConfigurationImpl clientConfigurationImpl0 = new ClientConfigurationImpl();
        final String string0 = clientConfigurationImpl0.getServerHost();
        assertNull(string0);
        clientConfigurationImpl0.setServerHost("test");
        assertEquals("test", clientConfigurationImpl0.getServerHost());
    }
}
