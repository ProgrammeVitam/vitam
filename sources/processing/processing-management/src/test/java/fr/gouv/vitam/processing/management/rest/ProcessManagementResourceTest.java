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
package fr.gouv.vitam.processing.management.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImpl;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImplFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.Collections;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, ProcessDistributorImplFactory.class})
public class ProcessManagementResourceTest {

    private static final String DATA_URI = "/processing/v1";
    private static final String NOT_EXITS_WORKFLOW_ID = "workflowJSONv3";
    private static final String EXITS_WORKFLOW_ID = "DefaultIngestWorkflow";
    private static final String URL_METADATA = "http://localhost:8086";
    private static final String URL_WORKSPACE = "http://localhost:8084";
    private static final String CONTAINER_NAME = "sipContainer";
    private static final String OPERATION_URI = "/operations";
    private static final String OPERATION_ID_URI = "/operations/xyz";
    private static final String ID = "identifier4";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static JunitHelper junitHelper;
    private static int port;
    private static ProcessManagementApplication application = null;
    private static final Integer TENANT_ID = 0;

    private static final String CONTEXT_ID = "INGEST";
    private static WorkspaceClient workspaceClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        final ServerConfiguration configuration = new ServerConfiguration();
        configuration.setUrlMetadata(URL_METADATA);
        configuration.setUrlWorkspace(URL_WORKSPACE);
        configuration.setJettyConfig(JETTY_CONFIG);
        application = new ProcessManagementApplication(configuration);
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
        } catch (final Exception e) {}
        junitHelper.releasePort(port);
    }

    /**
     * Test server status should return 200
     */
    @Test
    public void shouldGetStatusReturnNoContent() throws Exception {
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void shouldReturnResponseAccepted() throws Exception {

        WorkspaceClientFactory workspaceClientFactory = PowerMockito.mock(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = PowerMockito.mock(WorkspaceClient.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        Mockito.when(workspaceClient.getListUriDigitalObjectFromFolder(Mockito.anyObject(), Mockito.anyObject()))
            .thenReturn(new RequestResponseOK().addResult(Collections.<URI>emptyList()));

        // Mock ProcessDistributorImplFactory + ProcessDistributorImpl + Worker response
        PowerMockito.mockStatic(ProcessDistributorImplFactory.class);
        ProcessDistributorImplFactory processDistributorImplFactory =
            PowerMockito.mock(ProcessDistributorImplFactory.class);
        ProcessDistributorImpl processDistributorImpl = Mockito.mock(ProcessDistributorImpl.class);

        ItemStatus itemStatus = new ItemStatus();
        itemStatus.increment(StatusCode.OK);

        Mockito.when(processDistributorImplFactory.getDefaultDistributor()).thenReturn(processDistributorImpl);
        Mockito.when(processDistributorImpl.distribute(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject()))
            .thenReturn(itemStatus);


        final GUID processId = GUIDFactory.newGUID();
        final String operationByIdURI = OPERATION_URI + "/" + processId.getId();

        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.INIT.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID)).when()
            .post(operationByIdURI).then()
            .statusCode(Status.CREATED.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID)).when()
            .post(operationByIdURI).then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    /**
     * Test with an empty step in the workflow
     * @throws Exception
     */
    @Test
    public void shouldReturnPreconditionFailedIfNotStepFound() throws Exception {
        WorkspaceClientFactory workspaceClientFactory = PowerMockito.mock(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = PowerMockito.mock(WorkspaceClient.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        Mockito.when(workspaceClient.getListUriDigitalObjectFromFolder(Mockito.anyObject(), Mockito.anyObject()))
            .thenReturn(new RequestResponseOK().addResult(Collections.<URI>emptyList()));

        final GUID processId = GUIDFactory.newGUID();
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.INIT.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(CONTAINER_NAME, NOT_EXITS_WORKFLOW_ID)).when()
            .post(OPERATION_ID_URI).then()
            .statusCode(Status.CREATED.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(CONTAINER_NAME, NOT_EXITS_WORKFLOW_ID)).when()
            .post(OPERATION_ID_URI).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    /**
     * Try to resume an already running process
     * @throws Exception
     */
    @Test
    public void shouldReturnResponseUauthorized() throws Exception {

        WorkspaceClientFactory workspaceClientFactory = PowerMockito.mock(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = PowerMockito.mock(WorkspaceClient.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        Mockito.when(workspaceClient.getListUriDigitalObjectFromFolder(Mockito.anyObject(), Mockito.anyObject()))
            .thenReturn(new RequestResponseOK().addResult(Collections.<URI>emptyList()));

        // Mock ProcessDistributorImplFactory + ProcessDistributorImpl + Worker response
        PowerMockito.mockStatic(ProcessDistributorImplFactory.class);
        ProcessDistributorImplFactory processDistributorImplFactory =
            PowerMockito.mock(ProcessDistributorImplFactory.class);
        ProcessDistributorImpl processDistributorImpl = Mockito.mock(ProcessDistributorImpl.class);

        ItemStatus itemStatus = new ItemStatus();
        itemStatus.increment(StatusCode.OK);

        Mockito.when(processDistributorImplFactory.getDefaultDistributor()).thenReturn(processDistributorImpl);
        Mockito.when(processDistributorImpl.distribute(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject()))
            .thenReturn(itemStatus);


        final GUID processId = GUIDFactory.newGUID();
        final String operationByIdURI = OPERATION_URI + "/" + processId.getId();

        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.INIT.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID)).when()
            .post(operationByIdURI).then()
            .statusCode(Status.CREATED.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID)).when()
            .post(operationByIdURI).then()
            .statusCode(Status.ACCEPTED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry(processId.getId(), EXITS_WORKFLOW_ID)).when()
            .post(operationByIdURI).then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void shouldReturnResponseNOTFOUNDIfheadWorkflowByIdNotFound() throws Exception {
        given()
            .contentType(ContentType.JSON).when()
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.PAUSE.getValue(),
                GlobalDataRest.X_REQUEST_ID, ID, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .head(OPERATION_ID_URI).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Cancel an not existing process workflow
     * @throws Exception
     */
    @Test
    public void shouldReturnPreconditionFaildIfcancelWorkflowById() throws Exception {
        WorkspaceClientFactory workspaceClientFactory = PowerMockito.mock(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = PowerMockito.mock(WorkspaceClient.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        Mockito.when(workspaceClient.getListUriDigitalObjectFromFolder(Mockito.anyObject(), Mockito.anyObject()))
            .thenReturn(new RequestResponseOK().addResult(Collections.<URI>emptyList()));

        given()
            .contentType(ContentType.JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.PAUSE.getValue(),
                GlobalDataRest.X_REQUEST_ID, ID, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(ID).when()
            .delete(OPERATION_ID_URI).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    private static String generateResponseErrorFromStatus(Status status) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext("ingest").setState("code_vitam")
            .setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase()));
    }
}
