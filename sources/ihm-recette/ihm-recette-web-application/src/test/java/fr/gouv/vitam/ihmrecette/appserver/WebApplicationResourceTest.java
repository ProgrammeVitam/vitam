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

import java.io.File;
import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ihmrecette.soapui.SoapUiClient;
import fr.gouv.vitam.ihmrecette.soapui.SoapUiClientFactory;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({UserInterfaceTransactionManager.class, DslQueryHelper.class,
    IngestExternalClientFactory.class, SoapUiClientFactory.class})
// FIXME Think about Unit tests
public class WebApplicationResourceTest {

    // take it from conf file
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-recette";
    private static final String FAKE_OPERATION_ID = "1";
    private static JunitHelper junitHelper;
    private static int port;

    private static ServerApplicationWithoutMongo application;

    private static File adminConfigFile;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        final File adminConfig = PropertiesUtils.findFile("ihm-recette.conf");
        final WebApplicationConfig realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, WebApplicationConfig.class);
        realAdminConfig.setSipDirectory(Thread.currentThread().getContextClassLoader().getResource("sip").getPath());
        realAdminConfig.setSecure(false);
        adminConfigFile = File.createTempFile("test", "ihm-recette.conf", adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        try {
            application = new ServerApplicationWithoutMongo(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {

            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        application.stop();
        junitHelper.releasePort(port);
    }

    @Before
    public void initStaticMock() {
        PowerMockito.mockStatic(UserInterfaceTransactionManager.class);
        PowerMockito.mockStatic(DslQueryHelper.class);
        PowerMockito.mockStatic(IngestExternalClientFactory.class);
        PowerMockito.mockStatic(SoapUiClientFactory.class);
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
    
    @Test
    public void testLaunchSoapUiTestSuccess() throws IOException, InterruptedException {
        final SoapUiClient soapuiClient = PowerMockito.mock(SoapUiClient.class);
        final SoapUiClientFactory soapUiFactory = PowerMockito.mock(SoapUiClientFactory.class);

        PowerMockito.when(soapUiFactory.getClient()).thenReturn(soapuiClient);
        PowerMockito.when(SoapUiClientFactory.getInstance()).thenReturn(soapUiFactory);
        Mockito.doNothing().when(soapuiClient).launchTests();

        given().expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/soapui/launch");
    }
    
    @Test
    public void testLaunchSoapUiTestWhenThrowIOException() throws IOException, InterruptedException {
        final SoapUiClient soapuiClient = PowerMockito.mock(SoapUiClient.class);
        final SoapUiClientFactory soapUiFactory = PowerMockito.mock(SoapUiClientFactory.class);

        PowerMockito.when(soapUiFactory.getClient()).thenReturn(soapuiClient);
        PowerMockito.when(SoapUiClientFactory.getInstance()).thenReturn(soapUiFactory);
        Mockito.doThrow(IOException.class).when(soapuiClient).launchTests();

        given().expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/soapui/launch");
    }
    
    @Test
    public void testLaunchSoapUiTestWhenThrowInterruptedException() throws IOException, InterruptedException {
        final SoapUiClient soapuiClient = PowerMockito.mock(SoapUiClient.class);
        final SoapUiClientFactory soapUiFactory = PowerMockito.mock(SoapUiClientFactory.class);

        PowerMockito.when(soapUiFactory.getClient()).thenReturn(soapuiClient);
        PowerMockito.when(SoapUiClientFactory.getInstance()).thenReturn(soapUiFactory);
        Mockito.doThrow(InterruptedException.class).when(soapuiClient).launchTests();

        given().expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/soapui/launch");
    }
    

    
    @Test
    public void testGetLastReportWhenThrowIOException() throws InvalidParseOperationException {
        final SoapUiClient soapuiClient = PowerMockito.mock(SoapUiClient.class);
        final SoapUiClientFactory soapUiFactory = PowerMockito.mock(SoapUiClientFactory.class);

        PowerMockito.when(soapUiFactory.getClient()).thenReturn(soapuiClient);
        PowerMockito.when(SoapUiClientFactory.getInstance()).thenReturn(soapUiFactory);
        Mockito.doReturn(null).when(soapuiClient).getLastTestReport();

        given().expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/soapui/result");
    }
    
    @Test
    public void testGetLastReportWhenThrowInterruptedException() throws InvalidParseOperationException {
        final SoapUiClient soapuiClient = PowerMockito.mock(SoapUiClient.class);
        final SoapUiClientFactory soapUiFactory = PowerMockito.mock(SoapUiClientFactory.class);

        PowerMockito.when(soapUiFactory.getClient()).thenReturn(soapuiClient);
        PowerMockito.when(SoapUiClientFactory.getInstance()).thenReturn(soapUiFactory);
        Mockito.doThrow(InvalidParseOperationException.class).when(soapuiClient).getLastTestReport();

        given().expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/soapui/result");
    }
    
}
