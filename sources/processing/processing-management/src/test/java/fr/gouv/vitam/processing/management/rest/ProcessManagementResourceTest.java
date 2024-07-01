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
package fr.gouv.vitam.processing.management.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessManagementResourceTest extends ResteasyTestApplication {

    private static final String CONF_FILE_NAME = "processing.conf";

    private static final String DATA_URI = "/processing/v1";
    private static final String NOT_EXITS_WORKFLOW_ID = "workflowJSONv3";
    private static final String EXITS_WORKFLOW_ID = "PROCESS_SIP_UNITARY";
    private static final String CONTAINER_NAME = "sipContainer";
    private static final String OPERATION_URI = "/operations";
    private static final String WORKFLOWS_URI = "/workflows";
    private static final String OPERATION_ID_URI = "/operations/xyz";
    private static final String FORCE_PAUSE_URI = "/forcepause";
    private static final String ID = "identifier4";
    private static JunitHelper junitHelper;
    private static int port;
    private static ProcessManagementMain application = null;
    private static final Integer TENANT_ID = 0;

    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static final WorkerClientFactory workerClientFactory = mock(WorkerClientFactory.class);
    private static final ProcessManagement processManagement = mock(ProcessManagement.class);
    private static final ServerConfiguration serverConfiguration = mock(ServerConfiguration.class);

    {
        try {
            VitamApplicationInitializr.get().initialize(serverConfiguration, workerClientFactory, processManagement);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Object> getResources() {
        return VitamApplicationInitializr.get().getSingletons();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LogbookOperationsClientFactory.changeMode(null);
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        when(serverConfiguration.getWorkflowRefreshPeriod()).thenReturn(10000);
        when(serverConfiguration.getProcessingCleanerPeriod()).thenReturn(10000);
        when(serverConfiguration.getUrlWorkspace()).thenReturn("localhost");
        when(serverConfiguration.getUrlMetadata()).thenReturn("localhost");
        application = new ProcessManagementMain(CONF_FILE_NAME, ProcessManagementResourceTest.class, null);
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DATA_URI;
    }

    @AfterClass
    public static void shutdownAfterClass() {
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(port);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void before() {
        Mockito.reset(processManagement);
    }

    /**
     * Test server status should return 200
     */
    @Test
    public void shouldGetStatusReturnNoContent() {
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void shouldReturnResponseAccepted() throws Exception {
        ItemStatus itemStatus = new ItemStatus();
        itemStatus.increment(StatusCode.OK);
        itemStatus.setGlobalState(ProcessState.RUNNING);
        itemStatus.setLogbookTypeProcess(LogbookTypeProcess.INGEST.name());

        when(processManagement.init(any(), anyString())).thenReturn(new ProcessWorkflow());
        final GUID processId = GUIDFactory.newGUID();
        final String operationByIdURI = OPERATION_URI + "/" + processId.getId();

        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.INIT.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                processId.toString(),
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID))
            .when()
            .post(operationByIdURI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        when(processManagement.resume(any(), anyInt(), anyBoolean())).thenReturn(itemStatus);
        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                processId.toString(),
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID))
            .when()
            .post(operationByIdURI)
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    /**
     * Test with an empty step in the workflow
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnPreconditionFailedIfNotStepFound() throws Exception {
        doThrow(new ProcessingException("")).when(processManagement).init(any(), anyString());
        final GUID processId = GUIDFactory.newGUID();
        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.INIT.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                processId.toString(),
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(new ProcessingEntry(CONTAINER_NAME, NOT_EXITS_WORKFLOW_ID))
            .when()
            .post(OPERATION_ID_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /**
     * Try to resume an already running process
     *
     * @throws Exception
     */

    @Test
    public void shouldReturnResponseConflict() throws Exception {
        ItemStatus itemStatus = new ItemStatus();
        itemStatus.increment(StatusCode.OK);
        itemStatus.setGlobalState(ProcessState.RUNNING);
        itemStatus.setLogbookTypeProcess(LogbookTypeProcess.INGEST.name());

        final GUID processId = GUIDFactory.newGUID();
        final String operationByIdURI = OPERATION_URI + "/" + processId.getId();

        when(processManagement.init(any(), anyString())).thenReturn(new ProcessWorkflow());
        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.INIT.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                processId.toString(),
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID))
            .when()
            .post(operationByIdURI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        when(processManagement.resume(any(), anyInt(), anyBoolean())).thenReturn(itemStatus);
        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                processId.toString(),
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID))
            .when()
            .post(operationByIdURI)
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());

        doThrow(new StateNotAllowedException("")).when(processManagement).resume(any(), anyInt(), anyBoolean());
        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                processId.toString(),
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID))
            .when()
            .post(operationByIdURI)
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void shouldReturnResponseNOTFOUNDIfheadWorkflowByIdNotFound() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .when()
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.PAUSE.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                ID,
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .head(OPERATION_ID_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Cancel an not existing process workflow
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnPreconditionFaildIfcancelWorkflowById() throws Exception {
        doThrow(new ProcessingException("")).when(processManagement).cancel(anyString(), anyInt());
        given()
            .contentType(ContentType.JSON)
            .headers(
                GlobalDataRest.X_CONTEXT_ID,
                CONTEXT_ID,
                GlobalDataRest.X_ACTION,
                ProcessAction.PAUSE.getValue(),
                GlobalDataRest.X_REQUEST_ID,
                ID,
                GlobalDataRest.X_TENANT_ID,
                TENANT_ID
            )
            .body(ID)
            .when()
            .delete(OPERATION_ID_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Get the workflow definitions
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnOkWhenGetWorkflowDefinitions() throws Exception {
        Map<String, WorkFlow> map = new HashMap<>();
        map.put(EXITS_WORKFLOW_ID, new WorkFlow().setIdentifier(EXITS_WORKFLOW_ID));
        when(processManagement.getWorkflowDefinitions()).thenReturn(map);
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_REQUEST_ID, ID, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(WORKFLOWS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .assertThat()
            .body(containsString(EXITS_WORKFLOW_ID));
    }

    /**
     * Apply forced pause tests
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnGoodStatusWhenForcePause() throws Exception {
        doNothing().when(processManagement).forcePause(any());
        ProcessPause pause = new ProcessPause(null, 0, null);
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(pause)
            .when()
            .post(FORCE_PAUSE_URI)
            .then()
            .assertThat()
            .statusCode(Status.OK.getStatusCode());

        doThrow(new ProcessingException("be null")).when(processManagement).forcePause(any());
        //error when both tenant and type are null
        pause.setTenant(null);
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(pause)
            .when()
            .post(FORCE_PAUSE_URI)
            .then()
            .assertThat()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(containsString("be null"));

        doNothing().when(processManagement).forcePause(any());
        pause.setType("INGEST");
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(pause)
            .when()
            .post(FORCE_PAUSE_URI)
            .then()
            .assertThat()
            .statusCode(Status.OK.getStatusCode());

        doThrow(new ProcessingException("is not a valid process type")).when(processManagement).forcePause(any());
        //Inexisting type
        pause.setType("BAD_INGEST");
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(pause)
            .when()
            .post(FORCE_PAUSE_URI)
            .then()
            .assertThat()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(containsString("is not a valid process type"));
    }
}
