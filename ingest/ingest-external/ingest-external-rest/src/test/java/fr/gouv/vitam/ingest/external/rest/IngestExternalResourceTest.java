package fr.gouv.vitam.ingest.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.anyObject;

import java.io.File;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({IngestInternalClientFactory.class})
public class IngestExternalResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResourceTest.class);

    private static final String RESOURCE_URI = "/ingest-ext/v1";
    private static final String STATUS_URI = "/status";
    private static final String UPLOAD_URI = "/upload";
    private static final String INGEST_EXTERNAL_CONF = "ingest-external-test.conf";
    
    private static VitamServer vitamServer;
    private InputStream stream;
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
            if(vitamServer != null) {
                ((BasicVitamServer) vitamServer).stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
    }
    
    @Test
    public final void testGetStatus() {
        given()
        .when()
        .get(STATUS_URI)
        .then().statusCode(200);
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
    public void givenIngestInternalUploadErrorThenReturnInternalServerError() throws Exception {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fixed-virus.txt");
        
        PowerMockito.mockStatic(IngestInternalClientFactory.class);
        IngestInternalClient ingestInternalClient = PowerMockito.mock(IngestInternalClient.class);
        IngestInternalClientFactory ingestInternalFactory = PowerMockito.mock(IngestInternalClientFactory.class);
        PowerMockito.when(ingestInternalClient.upload(anyObject(), anyObject())).thenThrow(VitamException.class);
        PowerMockito.when(ingestInternalFactory.getIngestInternalClient()).thenReturn(ingestInternalClient);
        PowerMockito.when(IngestInternalClientFactory.getInstance()).thenReturn(ingestInternalFactory);
        
        given().contentType(ContentType.BINARY)
        .when().post(UPLOAD_URI)
        .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
    
    @Test
    public void givenAnInputstreamWhenUploadThenReturnErrorCode() {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("unfixed-virus.txt");
        
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(UPLOAD_URI)
        .then().statusCode(Status.OK.getStatusCode());
    }

}
