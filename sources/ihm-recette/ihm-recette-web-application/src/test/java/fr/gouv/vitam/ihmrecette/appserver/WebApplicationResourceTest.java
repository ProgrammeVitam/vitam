package fr.gouv.vitam.ihmrecette.appserver;
/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */


import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.anyObject;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({UserInterfaceTransactionManager.class, DslQueryHelper.class,
    IngestExternalClientFactory.class, JsonTransformer.class, WebApplicationConfig.class})
// FIXME Think about Unit tests
public class WebApplicationResourceTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/test-admin";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String DEFAULT_HOST = "localhost";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String FAKE_OPERATION_ID = "1";
    private static final String SAMPLE_LOGBOOKOPERATION_FILENAME = "logbookoperation_sample.json";
    private static final String SIP_DIRECTORY = "sip";
    private static JunitHelper junitHelper;
    private static int port;

    private static ServerApplication application;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        final WebApplicationConfig webApplicationConfig =
            (WebApplicationConfig) new WebApplicationConfig().setPort(port).setBaseUrl(DEFAULT_WEB_APP_CONTEXT)
                .setServerHost(DEFAULT_HOST).setStaticContent(DEFAULT_STATIC_CONTENT)
                .setSecure(false)
                .setSipDirectory(Thread.currentThread().getContextClassLoader().getResource(SIP_DIRECTORY).getPath())
                .setJettyConfig(JETTY_CONFIG);
        // FIXME Fix mongo conf in order to be able to run Unit tests
        application = new ServerApplication(webApplicationConfig);
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // FIXME Fix mongo conf in order to be able to run Unit tests
        application.stop();
        junitHelper.releasePort(port);
    }

    @Before
    public void initStaticMock() {
        PowerMockito.mockStatic(UserInterfaceTransactionManager.class);
        PowerMockito.mockStatic(DslQueryHelper.class);
        PowerMockito.mockStatic(IngestExternalClientFactory.class);
    }

    @Test
    public void givenNoSecureServerLoginUnauthorized() {
        given().contentType(ContentType.JSON).body(CREDENTIALS).expect().statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
        given().contentType(ContentType.JSON).body(CREDENTIALS_NO_VALID).expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode()).when()
            .post("login");
    }

    @Test
    @Ignore //FIXME  do unit test
    public void testSuccessStatus() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().get("status");
    }

    @Test
    @Ignore //FIXME  do unit test
    public void testDeleteFormatOK() throws Exception {
        // TODO to do
    }

    @Test
    @Ignore //FIXME  do unit test
    public void testDeleteRulesFileOK() throws Exception {
        // TODO to do
    }

    @Test
    public void testUploadFileFromServerSuccess() throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);

        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doReturn(Response.status(Status.OK).header(GlobalDataRest.X_REQUEST_ID, FAKE_OPERATION_ID)
            .build()).when(ingestClient).upload(anyObject());

        given().param("file_name", "SIP.zip").expect().statusCode(Status.OK.getStatusCode())
            .when()
            .get("/upload/SIP.zip");
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenFileNotFound() throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);

        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doReturn(Response.status(Status.OK).header(GlobalDataRest.X_REQUEST_ID, FAKE_OPERATION_ID)
            .build()).when(ingestClient).upload(anyObject());

        given().param("file_name", "SIP_NOT_FOUND.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP_NOT_FOUND.zip");
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenVitamException() throws Exception {
        final IngestExternalClient ingestClient = PowerMockito.mock(IngestExternalClient.class);
        final IngestExternalClientFactory ingestFactory = PowerMockito.mock(IngestExternalClientFactory.class);

        PowerMockito.when(ingestFactory.getClient()).thenReturn(ingestClient);
        PowerMockito.when(IngestExternalClientFactory.getInstance()).thenReturn(ingestFactory);
        Mockito.doThrow(VitamException.class).when(ingestClient).upload(anyObject());

        given().param("file_name", "SIP.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP.zip");
    }

    @Test
    public void testGetAvailableFilesListWithInternalSererWhenBadSipDirectory() {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory("SIP_DIRECTORY_NOT_FOUND");

        given().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/fileslist");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testGetAvailableFilesListWithInternalSererWhenNotConfiguredSipDirectory() {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory(null);

        given().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/fileslist");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenNotConfiguredSipDirectory() throws VitamException {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory(null);

        given().param("file_name", "SIP.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP.zip");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

}
