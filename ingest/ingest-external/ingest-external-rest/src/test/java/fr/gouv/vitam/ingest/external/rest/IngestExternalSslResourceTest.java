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
