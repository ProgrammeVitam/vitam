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
package fr.gouv.vitam.ingest.external.rest;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.SSLConfig;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;

public class IngestExternalSslResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalSslResourceTest.class);

    private static final String RESOURCE_URI = "/ingest-ext/v1";
    private static final String STATUS_URI = "/status";
    private static final String INGEST_EXTERNAL_CONF = "ingest-external-ssl-test.conf";

    private static VitamServer vitamServer;
    private static JunitHelper junitHelper;
    private static int serverPort;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        junitHelper = new JunitHelper();
        serverPort = junitHelper.findAvailablePort();
        //TODO verifier la compatibilité avec les test parallèle sur jenkins
        SystemPropertyUtil.set(VitamServer.PARAMETER_JETTY_SERVER_PORT, Integer.toString(serverPort));
        final File conf = PropertiesUtils.findFile(INGEST_EXTERNAL_CONF);

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;
       
        // TODO activate authentication
       // RestAssured.keystore("src/test/resources/tls/server/granted_certs.jks", "gazerty");
        try {
            vitamServer = IngestExternalApplication.startApplication(conf.getAbsolutePath());
            ((BasicVitamServer) vitamServer).start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Ingest External Application Server", e);
        }

    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (vitamServer != null) {
                ((BasicVitamServer) vitamServer).stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
    }

    @Test
    public final void testSSLGetStatus() {
        given()
            .relaxedHTTPSValidation("TLS")
            .when()
            .get("https://localhost:"+serverPort+RESOURCE_URI+STATUS_URI)
            .then()
            .statusCode(200);
    }

}
