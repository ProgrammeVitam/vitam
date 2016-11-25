/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.ingest.external.client;

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.client2.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ingest.external.rest.IngestExternalApplication;

/**
 *
 */
public class IngestExternalIT {

    private static final String INGEST_EXTERNAL_CONF = "ingest-external-ssl-test.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF = "ingest-external-client-secure.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_NOTGRANTED =
        "ingest-external-client-secure_notgranted.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_EXPIRED = "ingest-external-client-secure_expired.conf";

    private static IngestExternalApplication application;
    private static JunitHelper junitHelper;
    private static int serverPort;

    @BeforeClass
    public static void setUpBeforeClass() {

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        // TODO verifier la compatibilité avec les tests parallèles sur jenkins
        JunitHelper.setJettyPortSystemProperty(serverPort);

        application = new IngestExternalApplication(INGEST_EXTERNAL_CONF);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException(
                "Cannot start the Ingest External Application Server", e);
        }
    }



    @AfterClass
    public static void tearDown() throws Exception {
        if (application != null) {
            application.stop();
        }
        junitHelper.releasePort(serverPort);
    }

    @Test
    public void givenCertifValidThenReturnOK() {
        final SecureClientConfiguration secureClientConfiguration =
            IngestExternalClientFactory.changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF);
        secureClientConfiguration.setServerPort(serverPort);
        IngestExternalClientFactory.changeMode(secureClientConfiguration);

        try {
            IngestExternalClientFactory.getInstance().getClient().checkStatus();
        } catch (final VitamApplicationServerException e) {
            e.printStackTrace();
            fail();
        }
    }


    @Test(expected = VitamApplicationServerException.class)
    public void givenCertifNotGrantedThenReturnForbidden() throws VitamApplicationServerException {
        final SecureClientConfiguration secureClientConfiguration =
            IngestExternalClientFactory.changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOTGRANTED);
        secureClientConfiguration.setServerPort(serverPort);

        IngestExternalClientFactory.changeMode(secureClientConfiguration);
        IngestExternalClientFactory.getInstance().getClient().checkStatus();
    }


    @Test(expected = VitamApplicationServerException.class)
    public void givenCertifExpiredThenRaiseAnException() throws VitamApplicationServerException {
        final SecureClientConfiguration secureClientConfiguration =
            IngestExternalClientFactory.changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_EXPIRED);
        secureClientConfiguration.setServerPort(serverPort);

        IngestExternalClientFactory.changeMode(secureClientConfiguration);
        IngestExternalClientFactory.getInstance().getClient().checkStatus();
    }
}
