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
package fr.gouv.vitam.worker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.common.WorkerAccessRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkerClientRestTest extends ResteasyTestApplication {

    private static final String DUMMY_REQUEST_ID = "reqId";
    protected static int serverPort;
    protected static WorkerClientRest client;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    private static WorkerClientFactory factory = WorkerClientFactory.getInstance(
        WorkerClientFactory.changeConfigurationFile("worker-client.conf")
    );
    private static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        WorkerClientRestTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (WorkerClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/worker/v1")
    public static class MockResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("/tasks")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response submitStep(@Context HttpHeaders headers, JsonNode descriptionStepJson) {
            return expectedResponse.post();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void submitOK() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);

        final ItemStatus result = new ItemStatus("StepId");

        final ItemStatus itemStatus1 = new ItemStatus("checkSeda");
        itemStatus1.setMessage("CHECK_MANIFEST_OK");
        final StatusCode status = StatusCode.OK;
        itemStatus1.increment(status);
        result.setItemsStatus("checkSeda", itemStatus1);

        final ItemStatus itemStatus2 = new ItemStatus("CheckVersion");
        itemStatus2.setMessage("CHECK_VERSION_OK");
        itemStatus2.increment(status);
        result.setItemsStatus("CheckVersion", itemStatus2);

        when(mock.post()).thenReturn(Response.status(Response.Status.OK).entity(result).build());
        final ItemStatus responses = client.submitStep(
            new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters())
        );
        assertNotNull(responses);
        assertEquals(StatusCode.OK, responses.getItemsStatus().get("checkSeda").getGlobalStatus());
        assertEquals(StatusCode.OK, responses.getItemsStatus().get("CheckVersion").getGlobalStatus());
        assertEquals(StatusCode.OK, responses.getGlobalStatus());
    }

    @RunWithCustomExecutor
    @Test(expected = WorkerNotFoundClientException.class)
    public void submitNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
    }

    @RunWithCustomExecutor
    @Test(expected = WorkerServerClientException.class)
    public void submitException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
    }

    @RunWithCustomExecutor
    @Test
    public void submitAsyncDataRetryException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        List<WorkerAccessRequest> entity = List.of(
            new WorkerAccessRequest("accessRequestId1", "strategyId1", "offerId"),
            new WorkerAccessRequest("accessRequestId2", "strategyId1", "offerId"),
            new WorkerAccessRequest("accessRequestId3", "strategyId2", null)
        );

        when(mock.post()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_ASYNC_DATA_RETRY_LATER.getStatusCode())
                .entity(entity)
                .build()
        );
        Throwable thrown = catchThrowable(
            () -> client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()))
        );
        assertThat(thrown)
            .isInstanceOf(ProcessingRetryAsyncException.class)
            .hasFieldOrProperty("accessRequestIdByContext");
        ProcessingRetryAsyncException exc = (ProcessingRetryAsyncException) thrown;
        assertThat(exc.getAccessRequestIdByContext()).containsKey(new AccessRequestContext("strategyId1", "offerId"));
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("strategyId1", "offerId"))).contains(
            "accessRequestId1",
            "accessRequestId2"
        );
        assertThat(exc.getAccessRequestIdByContext()).containsKey(new AccessRequestContext("strategyId2", null));
        assertThat(exc.getAccessRequestIdByContext().get(new AccessRequestContext("strategyId2", null))).contains(
            "accessRequestId3"
        );
    }

    @RunWithCustomExecutor
    @Test
    public void submitAsyncDataRetryInvalidProcessingException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        List<WorkerAccessRequest> entity = List.of(new WorkerAccessRequest(null, "strategyId1", "offerId"));

        when(mock.post()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_ASYNC_DATA_RETRY_LATER.getStatusCode())
                .entity(entity)
                .build()
        );
        Throwable thrown = catchThrowable(
            () -> client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()))
        );
        assertThat(thrown).isInstanceOf(ProcessingException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void submitAsyncDataRetryNoDataProcessingException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);

        when(mock.post()).thenReturn(
            Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_ASYNC_DATA_RETRY_LATER.getStatusCode()).build()
        );
        Throwable thrown = catchThrowable(
            () -> client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()))
        );
        assertThat(thrown).isInstanceOf(ProcessingException.class);
    }
}
