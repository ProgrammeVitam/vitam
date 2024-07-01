/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.HeaderIdContainerFilter;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class IngestInternalResourceTest extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalResourceTest.class);

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        IngestInternalResourceTest.class
    );

    private static final String REST_URI = "/ingest/v1";
    private static final String STATUS_URI = "/status";
    private static final String INGEST_URL = "/ingests";
    private static final String DEFAULT_CONTEXT = "defaultContext";
    private static final String START_CONTEXT = "DEFAULT_WORKFLOW_RESUME";
    private static final String INIT_CONTEXT = "initContext";
    private static final String OPERATION_URL = "/operations/id1";

    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);

    private static final ProcessingManagementClientFactory processingManagementClientFactory = mock(
        ProcessingManagementClientFactory.class
    );

    private static final LogbookOperationsClientFactory logbookOperationsClientFactory = mock(
        LogbookOperationsClientFactory.class
    );

    private GUID ingestGuid;
    private ProcessingManagementClient processingClient;
    private WorkspaceClient workspaceClient;
    private LogbookOperationsClient logbookOperationsClient;

    private List<LogbookParameters> operationList = new ArrayList<>();
    private List<LogbookParameters> operationList2 = new ArrayList<>();

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(
            new HeaderIdContainerFilter(),
            new IngestInternalResource(
                workspaceClientFactory,
                processingManagementClientFactory,
                logbookOperationsClientFactory
            )
        );
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        RestAssured.port = vitamServerTestRunner.getBusinessPort();
        RestAssured.basePath = REST_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        LOGGER.debug("Ending tests");
        vitamServerTestRunner.runAfter();
    }

    @Before
    public void setUp() throws Exception {
        processingClient = mock(ProcessingManagementClient.class);
        workspaceClient = mock(WorkspaceClient.class);
        logbookOperationsClient = mock(LogbookOperationsClient.class);
        when(processingManagementClientFactory.getClient()).thenReturn(processingClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        ingestGuid = GUIDFactory.newManifestGUID(0);
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                ingestGuid
            );

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                ingestGuid
            );

        final LogbookOperationParameters externalOperationParameters3 =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest Launch internal",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest internal",
                ingestGuid
            );
        operationList = new ArrayList<>();
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        operationList.add(externalOperationParameters3);

        final LogbookOperationParameters externalOperationParameters4 =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external ATR",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "End Ingest ATR",
                ingestGuid
            );
        final LogbookOperationParameters externalOperationParameters5 =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest external ATR",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest ATR",
                ingestGuid
            );
        final LogbookOperationParameters externalOperationParameters6 =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0),
                "Ingest Launch internal",
                ingestGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest internal",
                ingestGuid
            );
        operationList2 = new ArrayList<>();
        operationList2.add(externalOperationParameters4);
        operationList2.add(externalOperationParameters5);
        operationList2.add(externalOperationParameters6);
    }

    @Test
    public void givenStartedServer_WhenGetStatus_ThenReturnStatusNoContent() throws Exception {
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void givenNoZipWhenUploadSipAsStreamThenReturnKO() {
        given()
            .body(operationList)
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post(INGEST_URL)
            .then()
            .statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void givenUnzipNonZipErrorWhenUploadSipAsStreaminInitPhaseThenReturnKO() throws Exception {
        doThrow(new BadRequestException("Test")).when(processingClient).initVitamProcess(any(), any());

        final InputStream inputStreamZip = PropertiesUtils.getResourceAsStream("SIP_mauvais_format.pdf");

        given()
            .headers(
                GlobalDataRest.X_REQUEST_ID,
                ingestGuid.getId(),
                GlobalDataRest.X_ACTION,
                ProcessAction.INIT,
                GlobalDataRest.X_ACTION_INIT,
                ProcessAction.INIT,
                GlobalDataRest.X_CONTEXT_ID,
                INIT_CONTEXT,
                GlobalDataRest.X_TYPE_PROCESS,
                "INGEST"
            )
            .body(inputStreamZip)
            .contentType(CommonMediaType.ZIP)
            .when()
            .post(INGEST_URL)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenUnzipObjectErrorWhenUploadSipAsStreamThenReturnKO() throws Exception {
        doThrow(new ContentAddressableStorageException("Test"))
            .when(workspaceClient)
            .uncompressObject(any(), any(), any(), any());

        try (InputStream inputStream = new FakeInputStream(1)) {
            given()
                .headers(
                    GlobalDataRest.X_REQUEST_ID,
                    ingestGuid.getId(),
                    GlobalDataRest.X_ACTION,
                    ProcessAction.RESUME,
                    GlobalDataRest.X_ACTION_INIT,
                    ProcessAction.START,
                    GlobalDataRest.X_CONTEXT_ID,
                    START_CONTEXT,
                    GlobalDataRest.X_TYPE_PROCESS,
                    "INGEST"
                )
                .body(inputStream)
                .contentType(CommonMediaType.ZIP)
                .when()
                .post(INGEST_URL)
                .then()
                .statusCode(Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

    @Test
    public void givenContainerAlreadyExistsWhenUploadSipAsStreamThenReturnKO() throws Exception {
        doReturn(true).when(workspaceClient).isExistingContainer(any());

        try (InputStream inputStream = PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip")) {
            given()
                .header(
                    GlobalDataRest.X_REQUEST_ID,
                    ingestGuid.getId(),
                    GlobalDataRest.X_ACTION,
                    ProcessAction.RESUME,
                    GlobalDataRest.X_CONTEXT_ID,
                    DEFAULT_CONTEXT
                )
                .body(inputStream)
                .contentType(CommonMediaType.ZIP)
                .when()
                .post(INGEST_URL)
                .then()
                .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void downloadObjects() {
        given()
            .when()
            .get(INGEST_URL + "/" + ingestGuid.getId() + "/" + DataCategory.REPORT.getCollectionName())
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .when()
            .get(INGEST_URL + "/" + ingestGuid.getId() + "/" + DataCategory.MANIFEST.getCollectionName())
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .when()
            .get(INGEST_URL + "/" + ingestGuid.getId() + "/" + DataCategory.LOGBOOK.getCollectionName())
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given()
            .when()
            .get(INGEST_URL + "/" + ingestGuid.getId() + "/unknown")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenOperationIdUnavailableWhenupdateOperationProcessStatusThenRaiseAnExceptionProcessingException()
        throws Exception {
        doThrow(new VitamClientException("")).when(processingClient).updateOperationActionProcess(any(), any());

        given()
            .headers(
                GlobalDataRest.X_REQUEST_ID,
                ingestGuid.getId(),
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME,
                GlobalDataRest.X_CONTEXT_ID,
                DEFAULT_CONTEXT
            )
            .when()
            .put(OPERATION_URL)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void givenOperationIdUnavailableWhengetStatusThenRaiseAnExceptionProcessingException() throws Exception {
        doThrow(new VitamClientException("")).when(processingClient).getOperationProcessStatus(any());

        given()
            .headers(
                GlobalDataRest.X_REQUEST_ID,
                ingestGuid.getId(),
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME,
                GlobalDataRest.X_CONTEXT_ID,
                DEFAULT_CONTEXT
            )
            .when()
            .head(OPERATION_URL)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void givenOperationIdWhengetDetailedStatusThenReturnOk() throws Exception {
        ItemStatus itemStatus = new ItemStatus().setGlobalState(ProcessState.COMPLETED);

        RequestResponseOK<ItemStatus> objectRequestResponseOK = new RequestResponseOK<ItemStatus>()
            .addResult(itemStatus)
            .setHttpCode(Status.OK.getStatusCode());
        when(processingClient.getOperationProcessExecutionDetails(anyString())).thenReturn(objectRequestResponseOK);

        given()
            .headers(
                GlobalDataRest.X_REQUEST_ID,
                ingestGuid.getId(),
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME,
                GlobalDataRest.X_CONTEXT_ID,
                DEFAULT_CONTEXT
            )
            .contentType(MediaType.APPLICATION_JSON)
            .body(JsonHandler.createObjectNode())
            .when()
            .get(OPERATION_URL)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void givenOperationIdWhengetStatusBadRequestExceptionThenReturnOk() throws Exception {
        when(processingClient.getOperationProcessStatus(any())).thenReturn(
            new ItemStatus().setGlobalState(ProcessState.RUNNING)
        );
        given()
            .headers(
                GlobalDataRest.X_REQUEST_ID,
                ingestGuid.getId(),
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME,
                GlobalDataRest.X_CONTEXT_ID,
                DEFAULT_CONTEXT
            )
            .when()
            .head(OPERATION_URL)
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void givenOperationIdWhenDeleteOperationProcessThenOK() throws Exception {
        ItemStatus result = new ItemStatus();
        result.setGlobalState(ProcessState.COMPLETED);
        result.increment(StatusCode.FATAL);
        result.setItemId("Item");

        RequestResponseOK<ItemStatus> responseOK = new RequestResponseOK<ItemStatus>().addResult(result);
        responseOK.setHttpCode(Status.ACCEPTED.getStatusCode());

        when(processingClient.cancelOperationProcessExecution(anyString())).thenReturn(responseOK);
        given()
            .headers(GlobalDataRest.X_REQUEST_ID, ingestGuid.getId())
            .when()
            .delete(OPERATION_URL)
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void givenWorkflowDefinitionsInternalServerExceptionThenReturnInternalServerError() throws Exception {
        when(processingClient.getWorkflowDefinitions()).thenThrow(new VitamClientException(""));
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .get("workflows")
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void givenWorkflowDefinitionsRequestResponseThenReturnOk() throws Exception {
        doReturn(
            new RequestResponseOK().addResult(JsonHandler.createObjectNode()).setHttpCode(Status.OK.getStatusCode())
        )
            .when(processingClient)
            .getWorkflowDefinitions();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .get("workflows")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }
}
