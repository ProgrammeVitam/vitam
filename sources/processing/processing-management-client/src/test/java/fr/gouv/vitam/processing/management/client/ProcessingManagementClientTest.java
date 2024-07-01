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
package fr.gouv.vitam.processing.management.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ProcessingManagementClientTest extends ResteasyTestApplication {

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    private static final ProcessingManagementClientFactory factory = ProcessingManagementClientFactory.getInstance();
    private static final VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        ProcessingManagementClientTest.class,
        factory
    );

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

        ProcessingResource(ExpectedResults mock) {
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
        public Response executeWorkFlow(
            @Context HttpHeaders headers,
            @PathParam("id") String id,
            ProcessingEntry process
        ) {
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
        public Response findProcessWorkflow(ProcessQuery query) {
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

    @Test
    public void test_is_operation_completed() throws Exception {
        // Test completed false
        when(mock.head()).thenReturn(
            Response.status(Status.ACCEPTED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.RUNNING.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .build()
        );
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            final ItemStatus operationProcessStatus = client.getOperationProcessStatus("FakeOp");
            final ProcessState state = operationProcessStatus.getGlobalState();
            assertEquals(ProcessState.RUNNING, state);
        }

        // Test completed true
        when(mock.head()).thenReturn(
            Response.status(Status.ACCEPTED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .build()
        );
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            final ItemStatus operationProcessStatus = client.getOperationProcessStatus("FakeOp");
            final ProcessState state = operationProcessStatus.getGlobalState();
            assertNotEquals(ProcessState.RUNNING, state);
        }

        // Test completed false
        when(mock.head()).thenReturn(
            Response.status(Status.ACCEPTED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.UNKNOWN)
                .build()
        );
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            final ItemStatus operationProcessStatus = client.getOperationProcessStatus("FakeOp");
            final ProcessState state = operationProcessStatus.getGlobalState();
            assertNotEquals(ProcessState.RUNNING, state);
        }

        // Test completed true
        when(mock.head()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .build()
        );
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            final ItemStatus operationProcessStatus = client.getOperationProcessStatus("FakeOp");
            final ProcessState state = operationProcessStatus.getGlobalState();
            assertNotEquals(ProcessState.RUNNING, state);
        }
    }

    @Test
    public void test_update_operation_ation_process() throws Exception {
        when(mock.put()).thenReturn(
            Response.status(Status.ACCEPTED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .entity(new RequestResponseOK<>())
                .build()
        );

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<ItemStatus> resp = client.updateOperationActionProcess("FakeAction", "FakeOp");
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.ACCEPTED.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");

        when(mock.put()).thenReturn(Response.status(Status.CONFLICT).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.updateOperationActionProcess("FakeAction", "FakeOp"))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Conflict");
        }

        when(mock.put()).thenReturn(Response.status(Status.PRECONDITION_FAILED).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.updateOperationActionProcess("FakeAction", "FakeOp"))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Precondition Failed");
        }

        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.updateOperationActionProcess("FakeAction", "FakeOp"))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Internal Server Error");
        }
    }

    @Test
    public void test_get_operation_process_status() throws Exception {
        when(mock.head()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_CONTEXT_ID, LogbookTypeProcess.INGEST)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .entity(new ItemStatus())
                .build()
        );

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            ItemStatus resp = client.getOperationProcessStatus("FakeOp");
            Assertions.assertThat(resp).isNotNull();
            Assertions.assertThat(resp.getGlobalStatus()).isEqualTo(StatusCode.OK);
        }

        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            client.getOperationProcessStatus("FakeOp");
            fail("should throw exception");
        } catch (WorkflowNotFoundException e) {
            //NOSONAR
        }

        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            client.getOperationProcessStatus("FakeOp");
            fail("should throw exception");
        } catch (BadRequestException e) {
            //NOSONAR
        }

        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            client.getOperationProcessStatus("FakeOp");
            fail("should throw exception");
        } catch (InternalServerException e) {
            //NOSONAR
        }
    }

    @Test
    public void test_get_operation_process_execution_details() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .entity(new RequestResponseOK<>())
                .build()
        );

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<ItemStatus> resp = client.getOperationProcessExecutionDetails("FakeOp");
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.OK.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");

        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.getOperationProcessExecutionDetails("FakeOp"))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Precondition Failed");
        }

        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.getOperationProcessExecutionDetails("FakeOp"))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Not Found");
        }

        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.getOperationProcessExecutionDetails("FakeOp"))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Internal Server Error");
        }
    }

    @Test
    public void test_cancel_operation_process_execution() throws Exception {
        when(mock.delete()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .entity(new RequestResponseOK<>())
                .build()
        );

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<ItemStatus> resp = client.cancelOperationProcessExecution("FakeOp");
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.OK.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");

        when(mock.delete()).thenReturn(Response.status(Status.CONFLICT).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.cancelOperationProcessExecution("FakeOp"))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Conflict");
        }

        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.cancelOperationProcessExecution("FakeOp")).isInstanceOf(
                VitamClientException.class
            );
        }

        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.cancelOperationProcessExecution("FakeOp"))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Internal Server Error");
        }
    }

    @Test
    public void test_init_vitam_process() throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE.name())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .entity(new RequestResponseOK<>())
                .build()
        );

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            ProcessingEntry processingEntry = new ProcessingEntry("FakeContainer", "FakeWorkflow");
            client.initVitamProcess(processingEntry);
            client.initVitamProcess("container", "workflowId");
        }

        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            ProcessingEntry processingEntry = new ProcessingEntry("FakeContainer", "FakeWorkflow");
            try {
                client.initVitamProcess(processingEntry);
                fail("should throw exception");
            } catch (WorkflowNotFoundException e) {
                //NOSONAR
            }
            try {
                client.initVitamProcess("container", "workflowId");
                fail("should throw exception");
            } catch (WorkflowNotFoundException e) {
                //NOSONAR
            }
        }

        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            ProcessingEntry processingEntry = new ProcessingEntry("FakeContainer", "FakeWorkflow");
            try {
                client.initVitamProcess(processingEntry);
                fail("should throw exception");
            } catch (IllegalArgumentException e) {
                //NOSONAR
            }
            try {
                client.initVitamProcess("container", "workflowId");
                fail("should throw exception");
            } catch (IllegalArgumentException e) {
                //NOSONAR
            }
        }

        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            ProcessingEntry processingEntry = new ProcessingEntry("FakeContainer", "FakeWorkflow");
            try {
                client.initVitamProcess(processingEntry);
                fail("should throw exception");
            } catch (BadRequestException e) {
                //NOSONAR
            }
            try {
                client.initVitamProcess("container", "workflowId");
                fail("should throw exception");
            } catch (BadRequestException e) {
                //NOSONAR
            }
        }

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            ProcessingEntry processingEntry = new ProcessingEntry("FakeContainer", "FakeWorkflow");
            try {
                client.initVitamProcess(processingEntry);
                fail("should throw exception");
            } catch (InternalServerException e) {
                //NOSONAR
            }
            try {
                client.initVitamProcess("container", "workflowId");
                fail("should throw exception");
            } catch (InternalServerException e) {
                //NOSONAR
            }
        }
    }

    @Test
    public void test_list_operations_details() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<ProcessDetail> resp = client.listOperationsDetails(new ProcessQuery());
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.OK.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.listOperationsDetails(new ProcessQuery()))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Internal Server Error");
        }
    }

    @Test
    public void test_get_workflow_definitions() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<WorkFlow> resp = client.getWorkflowDefinitions();
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.OK.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(client::getWorkflowDefinitions)
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Internal Server Error");
        }
    }

    @Test
    public void test_get_workflow_details() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new WorkFlow()).build());

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            Optional<WorkFlow> resp = client.getWorkflowDetails("WorkflowIdentifier");
            Assertions.assertThat(resp).isPresent();
        }

        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            Optional<WorkFlow> resp = client.getWorkflowDetails("WorkflowIdentifier");
            Assertions.assertThat(resp).isNotPresent();
        }

        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            client.getWorkflowDetails("WorkflowIdentifier");
            fail("should fail");
        } catch (VitamClientException e) {
            // NOSONAR
        }
    }

    @Test
    public void test_remove_force_pause() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<ProcessPause> resp = client.forcePause(new ProcessPause());
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.OK.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.forcePause(new ProcessPause()))
                .isInstanceOf(ProcessingException.class)
                .hasCauseInstanceOf(BadRequestException.class);
        }
    }

    @Test
    public void test_force_pause() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());

        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            RequestResponse<ProcessPause> resp = client.removeForcePause(new ProcessPause());
            Assertions.assertThat(resp.isOk()).isTrue();
            Assertions.assertThat(resp.getHttpCode()).isEqualTo(Status.OK.getStatusCode());
        }

        VitamError vitamError = new VitamError("status.name()")
            .setContext("INGEST")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(vitamError).build());
        try (
            ProcessingManagementClientRest client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient()
        ) {
            assertThatThrownBy(() -> client.removeForcePause(new ProcessPause()))
                .isInstanceOf(ProcessingException.class)
                .hasCauseInstanceOf(BadRequestException.class);
        }
    }
}
