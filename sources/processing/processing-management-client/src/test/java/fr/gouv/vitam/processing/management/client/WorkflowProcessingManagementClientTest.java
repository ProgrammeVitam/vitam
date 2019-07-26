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
package fr.gouv.vitam.processing.management.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


public class WorkflowProcessingManagementClientTest extends ResteasyTestApplication {
    private static final String WORKFLOWID = "json1";
    private static final String CONTAINER = "c1";
    private static final String ACTION_ID = "action1";
    private static final String ID = "id1";

    protected final static ExpectedResults mock = mock(ExpectedResults.class);

    static ProcessingManagementClientFactory factory = ProcessingManagementClientFactory.getInstance();
    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(WorkflowProcessingManagementClientTest.class, factory);


    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Before
    public void before() {
        reset(mock);
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new ProcessingResource(mock));
    }

    @Path("/processing/v1")
    public static class ProcessingResource {
        private final ExpectedResults mock;

        public ProcessingResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @Path("workflows")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkflowDefinitions() {
            return mock.get();
        }

        @Path("workflows/{workfowId}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkflowDefinitions(@PathParam("workfowId") String workfowId) {
            return mock.get();
        }

        @Path("operations/{id}")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id,
            ProcessingEntry process) {
            return mock.post();
        }

        @Path("operations/{id}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
            return mock.get();
        }

        @Path("operations/{id}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
            return mock.put();
        }

        @Path("operations/{id}")
        @DELETE
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response interruptWorkFlowExecution(@PathParam("id") String id) {
            return mock.delete();
        }

        @Path("operations/{id}")
        @HEAD
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowState(@PathParam("id") String id) {
            return mock.head();
        }

        @GET
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findProcessWorkflows(ProcessQuery query) {
            return mock.get();
        }

        @Path("/forcepause")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response forcePause(ProcessPause info) {
            return mock.post();
        }

        @Path("/removeforcepause")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response removeForcePause(ProcessPause info) {
            return mock.post();
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenUpdatingByIdThenReturnNotFound() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.updateOperationActionProcess(ACTION_ID, ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenUpdatingByIdThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.updateOperationActionProcess(ACTION_ID, ID);
        }
    }

    @Test
    public void givenUnauthorizedOperationWhenUpdatingByIdThenReturnUnauthorized() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            assertThatThrownBy(() -> client.updateOperationActionProcess(ACTION_ID, ID))
                .isInstanceOf(InternalServerException.class);
        }
    }

    @Test
    public void givenBadRequestWhenUpdatingByIdWorkFlowThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<ItemStatus> response = client.updateOperationActionProcess(ACTION_ID, ID);
            assertNotNull(response);
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenUpdatingByIdThenReturnInternalServerError() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.updateOperationActionProcess(ACTION_ID, ID);
        }
    }

    @Test
    public void updateOperationByIdProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<ItemStatus> response = client.updateOperationActionProcess(ACTION_ID, ID);
            assertNotNull(response);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenProcessingOperationThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.executeOperationProcess(ID, WORKFLOWID, ACTION_ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenProcessingOperationThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.executeOperationProcess(ID, WORKFLOWID, ACTION_ID);
        }
    }

    @Test(expected = NotAuthorizedException.class)
    public void givenUnauthorizedOperationWhenProcessingOperationThenReturnUnauthorized() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.executeOperationProcess(ID, WORKFLOWID, ACTION_ID);
        }
    }

    @Test()
    public void givenBadRequestWhenProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            final RequestResponse<ItemStatus> ret = client.executeOperationProcess(ID, WORKFLOWID, ACTION_ID);
            assertNotNull(ret);
            assertTrue(ret.isOk());
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ret.getHttpCode());
        }
    }

    @Test
    public void executeOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            final RequestResponse<ItemStatus> ret = client.executeOperationProcess(ID, WORKFLOWID, ACTION_ID);
            assertNotNull(ret);
            assertTrue(ret.isOk());
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenCancelProcessingOperationThenReturnNotFound() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.cancelOperationProcessExecution(ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenCancelProcessingOperationThenReturnIllegalPrecondtionFailed()
        throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.cancelOperationProcessExecution(ID);
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenIllegalOperationWhenCancelProcessingOperationThenReturnUnauthorized() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.cancelOperationProcessExecution(ID);
        }
    }

    @Test
    public void givenBadRequestWhenCancelProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.delete()).thenReturn(Response.status(Status.UNAUTHORIZED).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            assertThatThrownBy(() -> client.cancelOperationProcessExecution(ID))
                .isInstanceOf(InternalServerException.class);
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenCancelProcessingOperationThenReturnInternalServerError() throws
        Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.cancelOperationProcessExecution(ID);
        }
    }

    @Test
    public void CancelOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            final RequestResponse<ItemStatus> ret = client.cancelOperationProcessExecution(ID);
            assertNotNull(ret);
            assertTrue(ret.isOk());
        }
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenNotFoundWorkflowWhenHeadProcessingOperationStatusThenReturnNotFound() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessStatus(ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenHeadProcessingOperationStatusThenReturnIllegalPrecondtionFailed()
        throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessStatus(ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenUnauthorizedOperationWhenHeadProcessingOperationStatusThenReturnUnauthorized() throws
        Exception {
        when(mock.head()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessStatus(ID);
        }
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenHeadProcessingOperationStatusThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.head()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            final ItemStatus ret = client.getOperationProcessStatus(ID);
            assertNotNull(ret);
            assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenHeadProcessingOperationStatusThenReturnInternalServerError()
        throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessStatus(ID);
        }
    }

    @Test
    public void HeadProcessingOperationStatusOk() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.OK)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
            .header(GlobalDataRest.X_CONTEXT_ID, "Fake")
            .build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessStatus(ID);
        }
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenNotFoundWorkflowWhenGETProcessingOperationStatusThenReturnNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessExecutionDetails(ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenGETProcessingOperationStatusThenReturnIllegalPrecondtionFailed()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessExecutionDetails(ID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenUnauthorizedOperationWhenGETProcessingOperationStatusThenReturnUnauthorized() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessExecutionDetails(ID);
        }
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenGETProcessingOperationStatusThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            final ItemStatus ret = client.getOperationProcessExecutionDetails(ID);
            assertNotNull(ret);
            assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenGETProcessingOperationStatusThenReturnInternalServerError()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getOperationProcessExecutionDetails(ID);
        }
    }

    @Test
    public void GETProcessingOperationStatusOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            final ItemStatus ret = client.getOperationProcessExecutionDetails(ID);
            assertNotNull(ret);
            assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenInitProcessingOperationThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenInitProcessingOperationThenReturnIllegalPrecondtionFailed() throws
        Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenInitProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenInitProcessingOperationThenReturnInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }

    }

    @Test
    public void initOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }
    }

    @Test(expected = InternalServerException.class)
    public void giveninitWorkFlowOperationThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWheninitWorkFlowOperationThenReturnIllegalPrecondtionFailed() throws
        Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.initVitamProcess(CONTAINER, WORKFLOWID);
        }
    }

    @Test
    public void givenOKWhenDefinitionsWorkflowThenReturnMap() throws Exception {
        List<WorkFlow> desired = new ArrayList<>();
        desired.add(new WorkFlow().setId("TEST").setComment("TEST comment"));
        RequestResponseOK<WorkFlow> expected = new RequestResponseOK<>();
        expected.addAllResults(desired);
        expected.setHits(1, 0, 1, 1);
        expected.setHttpCode(Status.OK.getStatusCode());
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(expected).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<WorkFlow> requestResponse = client.getWorkflowDefinitions();
            assertEquals(expected.getHttpCode(), requestResponse.getHttpCode());
        }
    }

    @Test
    public void givenNotFoundWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
            assertEquals(Status.NOT_FOUND.getStatusCode(), requestReponse.getHttpCode());
        }
    }

    @Test
    public void givenPreconditionFailedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
            assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), requestReponse.getHttpCode());
        }
    }

    @Test
    public void givenUnauthaurizedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), requestReponse.getHttpCode());
        }
    }

    @Test
    public void givenInternalServerErrorWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner
            .getClient()) {
            RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), requestReponse.getHttpCode());
        }

    }

}
