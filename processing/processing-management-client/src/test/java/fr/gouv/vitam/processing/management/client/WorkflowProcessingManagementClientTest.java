package fr.gouv.vitam.processing.management.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;

public class WorkflowProcessingManagementClientTest extends JerseyTest {

    private static final String url = "http://localhost:8082";
    private static final ProcessingManagementClient client = new ProcessingManagementClient(url);
    private static final String WORKFLOWID = "json1";
    private static final String CONTAINER = "c1";
    Supplier<Response> mock;

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, "8082");
        mock = mock(Supplier.class);
        return new ResourceConfig().registerInstances(new ProcessingResource(mock));
    }

    @Path("/processing/api/v0.0.3")
    public static class ProcessingResource {
        private final Supplier<Response> expectedResponse;

        public ProcessingResource(Supplier<Response> expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("operations")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response executeVitamProcess(ProcessingEntry workflow) {
            return expectedResponse.get();
        }
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenNotFoundWorkflowWhenProcessingThenReturnNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenProcessingThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }

    @Test(expected = ProcessingException.class)
    public void givenUnauthorizedOperationWhenProcessingThenReturnUnauthorized() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }
}
