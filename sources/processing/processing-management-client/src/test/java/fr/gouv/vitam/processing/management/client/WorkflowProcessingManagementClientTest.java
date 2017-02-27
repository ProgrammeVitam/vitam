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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

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

import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnauthorizeException;


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

    @Test(expected = ProcessingInternalServerException.class)
    public void givenNotFoundWorkflowWhenProcessingThenReturnNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID, CONTEXT_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenProcessingThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID, CONTEXT_ID);
    }

    @Test(expected = ProcessingUnauthorizeException.class)
    public void givenUnauthorizedOperationWhenProcessingThenReturnUnauthorized() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID, CONTEXT_ID);
    }

    @Test
    public void givenBadRequestWhenProcessingThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        final Response ret = client.executeVitamProcess(CONTAINER, WORKFLOWID, CONTEXT_ID);
        assertNotNull(ret);
        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = ProcessingInternalServerException.class)
    public void givenInternalServerErrorWhenProcessingThenReturnInternalServerError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID, CONTEXT_ID);
    }

    @Test
    public void executeVitamProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(desired).build());
        final Response ret = client.executeVitamProcess(CONTAINER, WORKFLOWID, CONTEXT_ID);
        assertNotNull(ret);
        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
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

    @Test(expected = InternalServerException.class)
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
        client.updateOperationActionProcess(ACTION_ID, ID);
    }

    @Test
    public void givenBadRequestWhenUpdatingByIdWorkFlowThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        final Response response = client.updateOperationActionProcess(ACTION_ID, ID);
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
        final Response response = client.updateOperationActionProcess(ACTION_ID, ID);
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
        final Response ret = client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
        assertNotNull(ret);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), ret.getStatus());
        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = InternalServerException.class)
    public void givenInternalServerErrorWhenProcessingOperationThenReturnInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
    }

    @Test
    public void executeOperationProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        final Response ret = client.executeOperationProcess(ID, WORKFLOWID, CONTEXT_ID, ACTION_ID);
        assertNotNull(ret);
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

    @Test(expected = BadRequestException.class)
    public void givenBadRequestWhenCancelProcessingOperationThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.delete()).thenReturn(Response.status(Status.UNAUTHORIZED).entity(desired).build());
        final Response ret = client.cancelOperationProcessExecution(ID);
        assertNotNull(ret);
        // assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());s
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
        final Response ret = client.cancelOperationProcessExecution(ID);
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

    @Test(expected = InternalServerException.class)
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
        when(mock.head()).thenReturn(Response.status(Status.OK).build());
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

    @Test(expected = InternalServerException.class)
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
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);;

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
        client.initVitamProcess(CONTEXT_ID, CONTAINER, WORKFLOWID);;

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
        Response resp = client.initWorkFlow(CONTEXT_ID);
        assertEquals(resp.getStatus(), Status.NO_CONTENT.getStatusCode());

    }

    @Test
    public void givenInternalServerErrorWheninitWorkFlowThenReturnInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        Response resp = client.initWorkFlow(CONTEXT_ID);
        assertEquals(resp.getStatus(), Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void initWorkFlowProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(desired).build());
        Response resp = client.initWorkFlow(CONTEXT_ID);
        assertEquals(resp.getStatus(), Status.NO_CONTENT.getStatusCode());

    }


}
