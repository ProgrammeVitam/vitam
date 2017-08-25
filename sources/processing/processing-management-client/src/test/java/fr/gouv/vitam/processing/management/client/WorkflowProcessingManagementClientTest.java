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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.processing.common.ProcessingEntry;


public class WorkflowProcessingManagementClientTest extends VitamJerseyTest {
    private static ProcessingManagementClient client;
    private static final String WORKFLOWID = "json1";
    private static final String CONTAINER = "c1";
    private static final String ACTION_ID = "action1";
    private static final String CONTEXT_ID = "context1";
    private static final String ID = "id1";


    public WorkflowProcessingManagementClientTest() {
        super(ProcessingManagementClientFactory.getInstance());
    }

    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (ProcessingManagementClient) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        return new StartApplicationResponse<AbstractApplication>()
            .setServerPort(application.getVitamServer().getPort())
            .setApplication(application);
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new ProcessingResource(mock));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }


    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
    }


    @Path("/processing/v1")
    public static class ProcessingResource {
        private final ExpectedResults expectedResponse;

        public ProcessingResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("/workflows")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkflowDefinitions() {
            return expectedResponse.get();
        }

        @Path("operations")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response executeVitamProcess(ProcessingEntry workflow) {
            return expectedResponse.get();
        }

        @Path("operations")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateVitamProcess(@HeaderParam("X-CONTEXT-ID") String contextId,
            @HeaderParam("X-ACTION") String actionId, ProcessingEntry workflow) {
            return expectedResponse.put();
        }

        @Path("/operations/{id}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateOperationProcess(@HeaderParam("X-CONTEXT-ID") String contextId,
            @HeaderParam("X-ACTION") String actionId, ProcessingEntry workflow, @PathParam("id") String id) {
            return expectedResponse.put();
        }

        @Path("/operations/{id}")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response executeOperationProcess(@HeaderParam("X-CONTEXT-ID") String contextId,
            @HeaderParam("X-ACTION") String actionId, ProcessingEntry workflow, @PathParam("id") String id) {
            return expectedResponse.post();
        }

        @Path("/operations/{id}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
            return expectedResponse.get();
        }

        @Path("/operations/{id}")
        @DELETE
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response cancelOperationProcessExecution(@PathParam("id") String id) {
            return expectedResponse.delete();
        }

        @Path("/operations/{id}")
        @HEAD
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperationProcessStatus(@PathParam("id") String id) {
            return expectedResponse.head();
        }
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenUpdatingThenReturnNotFound() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.updateVitamProcess(CONTEXT_ID, ACTION_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenUpdatingThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.updateVitamProcess(CONTEXT_ID, ACTION_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenUnauthorizedOperationWhenUpdatingThenReturnUnauthorized() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.updateVitamProcess(CONTEXT_ID, ACTION_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenUpdatingWorkFlowThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        final ItemStatus ret = client.updateVitamProcess(CONTEXT_ID, ACTION_ID, CONTAINER, WORKFLOWID);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenUpdatingThenReturnInternalServerError() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.updateVitamProcess(CONTEXT_ID, ACTION_ID, CONTAINER, WORKFLOWID);
    }

    @Test
    public void updateVitamProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(desired).build());
        final ItemStatus ret = client.updateVitamProcess(ACTION_ID, CONTEXT_ID, CONTAINER, WORKFLOWID);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenUpdatingByIdThenReturnNotFound() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.updateOperationActionProcess(ACTION_ID, ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenUpdatingByIdThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.updateOperationActionProcess(ACTION_ID, ID);
    }

    @Test
    public void givenUnauthorizedOperationWhenUpdatingByIdThenReturnUnauthorized() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThatThrownBy(() -> client.updateOperationActionProcess(ACTION_ID, ID))
            .isInstanceOf(InternalServerException.class);

    }

    @Test
    public void givenBadRequestWhenUpdatingByIdWorkFlowThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        RequestResponse<ItemStatus> response = client.updateOperationActionProcess(ACTION_ID, ID);
        assertNotNull(response);
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenUpdatingByIdThenReturnInternalServerError() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.updateOperationActionProcess(ACTION_ID, ID);
    }

    @Test
    public void updateOperationByIdProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(desired).build());
        RequestResponse<ItemStatus> response = client.updateOperationActionProcess(ACTION_ID, ID);
        assertNotNull(response);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenProcessingOperationThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenProcessingOperationThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
    }

    @Test(expected = NotAuthorizedException.class)
    public void givenUnauthorizedOperationWhenProcessingOperationThenReturnUnauthorized() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
    }

    @Test()
    public void givenBadRequestWhenProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        final RequestResponse<JsonNode> ret = client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
        assertNotNull(ret);
        assertTrue(ret.isOk());


        assertEquals(Status.BAD_REQUEST.getStatusCode(), ret.getHttpCode());
        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test
    public void executeOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        final RequestResponse<JsonNode> ret = client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
        assertNotNull(ret);
        assertTrue(ret.isOk());

        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenNotFoundWorkflowWhenCancelProcessingOperationThenReturnNotFound() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.cancelOperationProcessExecution(ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenCancelProcessingOperationThenReturnIllegalPrecondtionFailed()
        throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.cancelOperationProcessExecution(ID);
    }

    @Test(expected = InternalServerException.class)
    public void givenIllegalOperationWhenCancelProcessingOperationThenReturnUnauthorized() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.cancelOperationProcessExecution(ID);
    }

    @Test
    public void givenBadRequestWhenCancelProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.delete()).thenReturn(Response.status(Status.UNAUTHORIZED).entity(desired).build());
        assertThatThrownBy(() -> client.cancelOperationProcessExecution(ID))
            .isInstanceOf(InternalServerException.class);
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenCancelProcessingOperationThenReturnInternalServerError() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.cancelOperationProcessExecution(ID);
    }

    @Test
    public void CancelOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.delete()).thenReturn(Response.status(Status.OK).entity(desired).build());
        final ItemStatus ret = client.cancelOperationProcessExecution(ID);
        assertNotNull(ret);
        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenNotFoundWorkflowWhenHeadProcessingOperationStatusThenReturnNotFound() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getOperationProcessStatus(ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenHeadProcessingOperationStatusThenReturnIllegalPrecondtionFailed()
        throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getOperationProcessStatus(ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenUnauthorizedOperationWhenHeadProcessingOperationStatusThenReturnUnauthorized() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.getOperationProcessStatus(ID);
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenHeadProcessingOperationStatusThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.head()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final ItemStatus ret = client.getOperationProcessStatus(ID);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenHeadProcessingOperationStatusThenReturnInternalServerError()
        throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getOperationProcessStatus(ID);
    }

    @Test
    public void HeadProcessingOperationStatusOk() throws Exception {
        when(mock.head()).thenReturn(Response.status(Status.OK)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
            .header(GlobalDataRest.X_CONTEXT_ID, "Fake")
            .build());

        client.getOperationProcessStatus(ID);
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenNotFoundWorkflowWhenGETProcessingOperationStatusThenReturnNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        JsonNode body = JsonHandler.createObjectNode();
        client.getOperationProcessExecutionDetails(ID, body);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenGETProcessingOperationStatusThenReturnIllegalPrecondtionFailed()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        JsonNode body = JsonHandler.createObjectNode();
        client.getOperationProcessExecutionDetails(ID, body);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenUnauthorizedOperationWhenGETProcessingOperationStatusThenReturnUnauthorized() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        JsonNode body = JsonHandler.createObjectNode();
        client.getOperationProcessExecutionDetails(ID, body);
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenGETProcessingOperationStatusThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        JsonNode body = JsonHandler.createObjectNode();
        final ItemStatus ret = client.getOperationProcessExecutionDetails(ID, body);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenGETProcessingOperationStatusThenReturnInternalServerError()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        JsonNode body = JsonHandler.createObjectNode();
        client.getOperationProcessExecutionDetails(ID, body);
    }

    @Test
    public void GETProcessingOperationStatusOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(desired).build());
        JsonNode body = JsonHandler.createObjectNode();
        final ItemStatus ret = client.getOperationProcessExecutionDetails(ID, body);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenInitProcessingOperationThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenInitProcessingOperationThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenInitProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenInitProcessingOperationThenReturnInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);

    }

    @Test
    public void initOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = InternalServerException.class)
    public void giveninitWorkFlowOperationThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWheninitWorkFlowOperationThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);
    }


    @Test
    public void givenBadRequestWheninitWorkFlowOperationThenReturnNoContent() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        client.initWorkFlow(CONTEXT_ID);

    }

    @Test
    public void givenInternalServerErrorWheninitWorkFlowThenReturnInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.initWorkFlow(CONTEXT_ID);
    }

    @Test
    public void initWorkFlowProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        client.initWorkFlow(CONTEXT_ID);

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
        RequestResponse<WorkFlow> requestResponse = client.getWorkflowDefinitions();
        assertEquals(expected.getHttpCode(), requestResponse.getHttpCode());
    }

    @Test
    public void givenNotFoundWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.NOT_FOUND.getStatusCode(), requestReponse.getHttpCode());
    }

    @Test
    public void givenPreconditionFailedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), requestReponse.getHttpCode());
    }

    @Test
    public void givenUnauthaurizedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), requestReponse.getHttpCode());
    }

    @Test
    public void givenInternalServerErrorWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), requestReponse.getHttpCode());

    }

}
