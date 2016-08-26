package fr.gouv.vitam.ingest.external.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;

public class IngestExternalResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResourceTest.class);

    private static final String RESOURCE_URI = "/ingest-ext/v1";
    private static final String STATUS_URI = "/status";
    private static final String UPLOAD_URI = "/upload";
    private static final String INGEST_EXTERNAL_CONF = "ingest-external.conf";
    
    private static VitamServer vitamServer;
    private InputStream stream;
    private IngestExternalApplication ingestExternalApplication;
    private static JunitHelper junitHelper;
    private static int serverPort;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        serverPort = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(INGEST_EXTERNAL_CONF);

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        try {
            vitamServer = IngestExternalApplication.startApplication(new String[] {
                conf.getAbsolutePath(),
                Integer.toString(serverPort)});
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
            if(vitamServer!=null) {
                ((BasicVitamServer) vitamServer).stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
    }

    @Test
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(200);
    }
    
    @Test
    public void givenAnInputstreamWhenUploadThenReturnOK() {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("no-virus.txt");
        
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(UPLOAD_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }
    
    @Test
    public void givenAnInputstreamWhenUploadAndFixVirusThenReturnOK() {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fixed-virus.txt");
        
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(UPLOAD_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }
    
    @Test
    public void givenAnInputstreamWhenUploadThenReturnErrorCode() {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("unfixed-virus.txt");
        
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(UPLOAD_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }

}
