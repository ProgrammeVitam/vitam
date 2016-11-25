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
package fr.gouv.vitam.ingest.internal.upload.rest;

import static com.jayway.restassured.RestAssured.get;
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
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server2.RequestIdContainerFilter;
import fr.gouv.vitam.common.server2.VitamServer;
import fr.gouv.vitam.common.server2.VitamServerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

public class IngestInternalResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalResourceTest.class);

    private static final String REST_URI = "/ingest/v1";
    private static final String STATUS_URI = "/status";
    private static final String LOGBOOK_URL = "/logbooks";
    private static final String INGEST_URL = "/ingests";

    private GUID ingestGuid;
    private static VitamServer vitamServer;
    private static int port;
    private static JunitHelper junitHelper;

    private static WorkspaceClient workspaceClient;
    private static ProcessingManagementClient processingClient;

    private List<LogbookParameters> operationList = new ArrayList<>();
    private List<LogbookParameters> operationList2 = new ArrayList<>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            vitamServer = buildTestServer();
            vitamServer.start();

            RestAssured.port = port;
            RestAssured.basePath = REST_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Ingest Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (vitamServer != null) {
                vitamServer.stop();
            }
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

    }

    @Before
    public void setUp() throws Exception {

        ingestGuid = GUIDFactory.newManifestGUID(0);
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                ingestGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                ingestGuid);

        final LogbookOperationParameters externalOperationParameters3 =
            LogbookParametersFactory.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest Launch internal",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest internal",
                ingestGuid);
        operationList = new ArrayList<>();
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        operationList.add(externalOperationParameters3);

        final LogbookOperationParameters externalOperationParameters4 =
            LogbookParametersFactory.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external ATR",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "End Ingest ATR",
                ingestGuid);
        final LogbookOperationParameters externalOperationParameters5 =
            LogbookParametersFactory.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external ATR",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest ATR",
                ingestGuid);
        final LogbookOperationParameters externalOperationParameters6 =
            LogbookParametersFactory.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest Launch internal",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest internal",
                ingestGuid);
        operationList2 = new ArrayList<>();
        operationList2.add(externalOperationParameters4);
        operationList2.add(externalOperationParameters5);
        operationList2.add(externalOperationParameters6);
    }


    // TODO P1: It would be better to use the test server / application here.
    private static VitamServer buildTestServer() throws VitamApplicationServerException {
        final VitamServer vitamServer = VitamServerFactory.newVitamServer(port);
        workspaceClient = mock(WorkspaceClient.class);
        processingClient = mock(ProcessingManagementClient.class);

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        final IngestInternalConfiguration configuration = new IngestInternalConfiguration();
        // url is here just for validation, not used
        configuration.setWorkspaceUrl("http://localhost:8888");
        configuration.setProcessingUrl("http://localhost:9999");

        resourceConfig.register(new IngestInternalResource(workspaceClient, processingClient));
        resourceConfig.register(RequestIdContainerFilter.class);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(sh, "/*");

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {contextHandler});
        vitamServer.configure(contextHandler);
        return vitamServer;
    }

    @Test
    public void givenStartedServer_WhenGetStatus_ThenReturnStatusNoContent() throws Exception {
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }


    @Test
    public void givenAllServicesAvailableAndVirusWhenUploadSipAsAsyncStreamThenReturnOK() throws Exception {
        reset(workspaceClient);
        reset(processingClient);

        Mockito.doReturn(false).when(workspaceClient).isExistingContainer(Matchers.anyObject());
        Mockito.doNothing().when(workspaceClient).createContainer(Matchers.anyObject());
        Mockito.doNothing().when(workspaceClient).uncompressObject(Matchers.anyObject(), Matchers.anyObject(),
            Matchers.anyObject(),
            Matchers.anyObject());

        final GUID processId = GUIDFactory.newGUID();

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(JsonHandler.unprettyPrint(operationList)).contentType(MediaType.APPLICATION_JSON)
            .then().statusCode(Status.CREATED.getStatusCode())
            .when().post(LOGBOOK_URL);

        final ItemStatus itemStatus = new ItemStatus(processId.toString()).increment(StatusCode.OK);
        Mockito.doReturn(itemStatus).when(processingClient).executeVitamProcess(Matchers.anyObject(),
            Matchers.anyObject());

        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStream).contentType(CommonMediaType.ZIP)
            .then().statusCode(Status.OK.getStatusCode())
            .when().post(INGEST_URL);

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(JsonHandler.unprettyPrint(operationList2)).contentType(MediaType.APPLICATION_JSON)
            .then().statusCode(Status.OK.getStatusCode())
            .when().put(LOGBOOK_URL);
    }


    @Test
    public void givenNoZipWhenUploadSipAsStreamThenReturnKO()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);

        RestAssured.given()
            .body(operationList).contentType(MediaType.APPLICATION_JSON)
            .when().post(INGEST_URL)
            .then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void givenUnzipNonZipErrorWhenUploadSipAsStreamThenReturnKO()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ContentAddressableStorageCompressedFileException("Test")).when(workspaceClient)
            .uncompressObject(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyObject());

        final GUID processId = GUIDFactory.newGUID();
        final ItemStatus itemStatus = new ItemStatus(processId.toString()).increment(StatusCode.OK);
        Mockito.doReturn(itemStatus).when(processingClient).executeVitamProcess(Matchers.anyObject(),
            Matchers.anyObject());

        final InputStream inputStreamZip =
            PropertiesUtils.getResourceAsStream("SIP_mauvais_format.pdf");

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStreamZip).contentType(CommonMediaType.ZIP)
            .when().post(INGEST_URL)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    @Test
    public void givenUnzipObjectErrorWhenUploadSipAsStreamThenReturnKO() throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ContentAddressableStorageServerException("Test")).when(workspaceClient)
            .uncompressObject(Matchers.anyObject(), Matchers.anyObject(), Matchers.anyObject(), Matchers.anyObject());

        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStream).contentType(CommonMediaType.ZIP)
            .when().post(INGEST_URL)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }


    @Test
    public void givenContainerAlreadyExistsWhenUploadSipAsStreamThenReturnKO() throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doReturn(true).when(workspaceClient).isExistingContainer(Matchers.anyObject());

        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStream).contentType(CommonMediaType.ZIP)
            .when().post(INGEST_URL)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    @Test
    public void givenProcessBadRequestWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ProcessingBadRequestException("Test")).when(processingClient).executeVitamProcess(
            Matchers.anyObject(),
            Matchers.anyObject());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStream).contentType(CommonMediaType.ZIP)
            .when().post(INGEST_URL)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void givenProcessInternalExceptionWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ProcessingInternalServerException("Test1")).when(processingClient).executeVitamProcess(
            Matchers.anyObject(),
            Matchers.anyObject());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStream).contentType(CommonMediaType.ZIP)
            .when().post(INGEST_URL)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void givenProcessUnavailableWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {
        reset(workspaceClient);
        reset(processingClient);
        Mockito.doThrow(new ProcessingException("")).when(processingClient).executeVitamProcess(Matchers.anyObject(),
            Matchers.anyObject());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given().header(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .body(inputStream).contentType(CommonMediaType.ZIP)
            .when().post(INGEST_URL)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}

