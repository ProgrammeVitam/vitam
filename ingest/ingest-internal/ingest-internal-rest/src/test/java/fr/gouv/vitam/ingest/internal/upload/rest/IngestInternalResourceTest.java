/**
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
 */
package fr.gouv.vitam.ingest.internal.upload.rest;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.ingest.internal.model.UploadResponseDTO;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageZipException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

public class IngestInternalResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalResourceTest.class);

    private static final String REST_URI = "/ingest/v1";
    private static final String STATUS_URI = "/status";
    private static final String UPLOAD_URI = "/upload";

    private static VitamServer vitamServer;
    private static int port;
    private static JunitHelper junitHelper;

    private static WorkspaceClient workspaceClient;
    private static ProcessingManagementClient processingClient;

    private List<LogbookParameters> operationList = new ArrayList<LogbookParameters>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        try {
            vitamServer = buildTestServer();
            ((BasicVitamServer) vitamServer).start();

            RestAssured.port = port;
            RestAssured.basePath = REST_URI;

            LOGGER.debug("Beginning tests");
        } catch (VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Ingest Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if(vitamServer != null) {
                ((BasicVitamServer) vitamServer).stop();
            }
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

    }

    @Before
    public void setUp() throws Exception {

        GUID ingestGuid = GUIDFactory.newGUID();
        GUID conatinerGuid = GUIDFactory.newGUID();
        LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                LogbookOutcome.STARTED,
                "Start Ingest external",
                conatinerGuid);

        LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                LogbookOutcome.OK,
                "End Ingest external",
                conatinerGuid);
        operationList = new ArrayList<LogbookParameters>();
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

    }


    private static VitamServer buildTestServer() throws VitamApplicationServerException {
        VitamServer vitamServer = VitamServerFactory.newVitamServer(port);
        workspaceClient = mock(WorkspaceClient.class);
        processingClient = mock(ProcessingManagementClient.class);

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        IngestInternalConfiguration configuration = new IngestInternalConfiguration();
        // url is here just for validation, not used
        configuration.setWorkspaceUrl("http://localhost:8888");
        configuration.setProcessingUrl("http://localhost:9999");

        resourceConfig.register(new IngestInternalResource(configuration, workspaceClient, processingClient));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(sh, "/*");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {contextHandler});
        vitamServer.configure(contextHandler);
        return vitamServer;
    }

    @Test
    public void givenStartedServer_WhenGetStatus_ThenReturnStatusOk() throws Exception {
        get(STATUS_URI).then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void givenAllServicesAvailableAndNoVirusWhenUploadSipAsStreamThenReturnOK() throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doReturn(false).when(workspaceClient).isExistingContainer(Mockito.anyObject());
        Mockito.doNothing().when(workspaceClient).createContainer(Mockito.anyObject());
        Mockito.doNothing().when(workspaceClient).unzipObject(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject());

        Mockito.doReturn("OK").when(processingClient).executeVitamProcess(Mockito.anyObject(),
            Mockito.anyObject());

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given()

            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI);

        inputStream.close();
    }

    @Test
    public void givenNoZipWhenUploadSipAsStreamThenReturnKO()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        
        UploadResponseDTO uploadResponseDTOReal = RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI).andReturn().getBody().as(UploadResponseDTO.class);

        assertNotNull(uploadResponseDTOReal);
        assertEquals(uploadResponseDTOReal.getVitamCode(), "500");
        assertEquals(uploadResponseDTOReal.getVitamStatus(), "upload failed");
        assertEquals(uploadResponseDTOReal.getMessage(), "file upload finished in error");

    }
    
    @Test
    public void givenUnzipNonZipErrorWhenUploadSipAsStreamThenReturnKO()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ContentAddressableStorageZipException("Test")).when(workspaceClient)
            .unzipObject(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject());

        Mockito.doReturn("OK").when(processingClient).executeVitamProcess(Mockito.anyObject(),
            Mockito.anyObject());

        InputStream inputStreamZip =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_mauvais_format.pdf");

        UploadResponseDTO uploadResponseDTOReal = RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_mauvais_format", inputStreamZip)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI).andReturn().getBody().as(UploadResponseDTO.class);

        assertNotNull(uploadResponseDTOReal);
        assertEquals(uploadResponseDTOReal.getVitamCode(), "500");
        assertEquals(uploadResponseDTOReal.getVitamStatus(), "workspace failed");
        assertEquals(uploadResponseDTOReal.getMessage(), "Test");

        inputStreamZip.close();
    }

    @Test
    public void givenUnzipObjectErrorWhenUploadSipAsStreamThenReturnKO() throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ContentAddressableStorageServerException("Test")).when(workspaceClient)
            .unzipObject(Matchers.anyObject(), Matchers.anyObject(), Matchers.anyObject());

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        UploadResponseDTO uploadResponseDTOReal = RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI).andReturn().getBody().as(UploadResponseDTO.class);

        assertNotNull(uploadResponseDTOReal);
        assertEquals(uploadResponseDTOReal.getVitamCode(), "500");
        assertEquals(uploadResponseDTOReal.getVitamStatus(), "workspace failed");
        assertEquals(uploadResponseDTOReal.getMessage(), "Test");

        inputStream.close();

    }


    @Test
    public void givenContainerAlreadyExistsWhenUploadSipAsStreamThenReturnKO() throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doReturn(true).when(workspaceClient).isExistingContainer(Mockito.anyObject());

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI);

        inputStream.close();

    }

    @Test
    public void givenProcessBadRequestWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ProcessingBadRequestException("Test")).when(processingClient).executeVitamProcess(
            Mockito.anyObject(),
            Mockito.anyObject());
        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        UploadResponseDTO uploadResponseDTOReal = RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI).andReturn().getBody().as(UploadResponseDTO.class);

        assertNotNull(uploadResponseDTOReal);
        assertEquals(uploadResponseDTOReal.getVitamCode(), "500");
        assertEquals(uploadResponseDTOReal.getVitamStatus(), "processing failed");
        assertEquals(uploadResponseDTOReal.getMessage(), "process workflow finished in error");
    }

    @Test
    public void givenProcessInternalExceptionWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ProcessingInternalServerException("Test")).when(processingClient).executeVitamProcess(
            Mockito.anyObject(),
            Mockito.anyObject());
        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        UploadResponseDTO uploadResponseDTOReal = RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI).andReturn().getBody().as(UploadResponseDTO.class);

        assertNotNull(uploadResponseDTOReal);
        assertEquals(uploadResponseDTOReal.getVitamCode(), "500");
        assertEquals(uploadResponseDTOReal.getVitamStatus(), "processing failed");
        assertEquals(uploadResponseDTOReal.getMessage(), "process workflow finished in error");
    }

    @Test
    public void givenProcessUnavailableWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ProcessingException("")).when(processingClient).executeVitamProcess(Mockito.anyObject(),
            Mockito.anyObject());
        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI).andReturn().getBody().asString().contains("upload failed");
    }


    @Test
    public void givenAllServicesAvailableAndVirusWhenUploadSipAsStreamThenReturnOK() throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doReturn(false).when(workspaceClient).isExistingContainer(Mockito.anyObject());
        Mockito.doNothing().when(workspaceClient).createContainer(Mockito.anyObject());
        Mockito.doNothing().when(workspaceClient).unzipObject(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject());

        Mockito.doReturn("OK").when(processingClient).executeVitamProcess(Mockito.anyObject(),
            Mockito.anyObject());

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given()
            .multiPart("part", operationList, MediaType.APPLICATION_JSON)
            .multiPart("part", "SIP_bordereau_avec_objet_OK", inputStream)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(UPLOAD_URI);
    }

}

