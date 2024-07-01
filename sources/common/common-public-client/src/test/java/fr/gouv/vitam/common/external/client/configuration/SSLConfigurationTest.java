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

import fr.gouv.vitam.common.exception.VitamException;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SSLConfigurationTest {

    private static List<SSLKey> keystore;
    private static List<SSLKey> truststore;
    private static SSLKey key;
    private static SSLContext context;

    @Before
    public void setUp() throws Exception {
        key = new SSLKey("tls/client/client.p12", "azerty4");
    }

    @Test
    public void testSSLConfiguration() throws Exception {
        SSLConfiguration config = new SSLConfiguration();
        assertNull(config.getKeystore());
        // test of emptyness
        try {
            context = config.createSSLContext();
            fail("Should raized an exception");
        } catch (final VitamException e) {}

        truststore = new ArrayList<>();
        keystore = new ArrayList<>();
        truststore.add(key);
        keystore.add(key);

        config = new SSLConfiguration(keystore, truststore);
        config.setKeystore(keystore);
        config.setTruststore(truststore);
        context = config.createSSLContext();

        assertEquals(1, config.getTruststore().size());
        assertEquals(1, config.getKeystore().size());
        assertNotNull(context);
    }
}
