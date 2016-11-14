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
package fr.gouv.vitam.access.external.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client2.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;

public class AccessExternalIT {

    private static final String ACCESS_EXTERNAL_CONF = "access-external-test-ssl.conf";
    private static final String ACCESS_EXTERNAL_CLIENT_CONF = "access-external-client-secure.conf";
    private static final String ACCESS_EXTERNAL_CLIENT_CONF_EXPIRED = "access-external-client-secure_expired.conf";
    private static JunitHelper junitHelper;
    private static int serverPort;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(ACCESS_EXTERNAL_CONF);

        try {
            AccessExternalApplication application = new AccessExternalApplication(conf.getAbsolutePath());
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException(
                "Cannot start the Access External Application Server", e);
        }
    }
    @Test
    public void givenCertifValidThenReturnOK() throws FileNotFoundException, IOException {
        final AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
        SecureClientConfigurationImpl secureConfig = PropertiesUtils.readYaml(PropertiesUtils.findFile(ACCESS_EXTERNAL_CLIENT_CONF),
            SecureClientConfigurationImpl.class);
        secureConfig.setServerPort(serverPort);
        AccessExternalClientFactory.changeMode(secureConfig);
        try (final AccessExternalClient client = factory.getClient()) {
            client.checkStatus();
        } catch (final VitamException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void givenCertifExpiredThenReturnKO() throws FileNotFoundException, IOException {
        final AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
        SecureClientConfigurationImpl secureConfig = PropertiesUtils.readYaml(PropertiesUtils.findFile(ACCESS_EXTERNAL_CLIENT_CONF_EXPIRED),
            SecureClientConfigurationImpl.class);
        secureConfig.setServerPort(serverPort);
        AccessExternalClientFactory.changeMode(secureConfig);
        try (final AccessExternalClient client = factory.getClient()) {
            client.checkStatus();
            fail();
        } catch (final VitamException e) {
        }
    }
}
