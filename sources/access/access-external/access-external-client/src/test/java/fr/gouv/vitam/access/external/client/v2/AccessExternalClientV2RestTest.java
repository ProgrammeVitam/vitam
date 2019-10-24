package fr.gouv.vitam.access.external.client.v2;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessExternalClientV2RestTest extends ResteasyTestApplication {
    protected static AccessExternalClientV2Rest client;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], \"$projection\" : {} }";
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    private final static ExpectedResults mock = mock(ExpectedResults.class);
    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(AccessExternalClientV2RestTest.class, AccessExternalClientV2Factory.getInstance());


    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        client = (AccessExternalClientV2Rest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/access-external/v2")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }


        @POST
        @Path("/dipexport")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response exportDIP(DipRequest dipRequest) {
            return expectedResponse.post();
        }
    }


    /***
     *
     * DIP export
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void exportDIP() throws Exception {
        when(mock.post())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        DipRequest exportRequest = new DipRequest(JsonHandler.getFromString(queryDsql));
        assertThat(client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            exportRequest)).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNotFoundThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        DipRequest exportRequest = new DipRequest(JsonHandler.getFromString(queryDsql));
        assertThat(client
            .exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), exportRequest)
            .getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNoQueryThen415() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(
            client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), (DipRequest) null).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPBadQueryThenPreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        DipRequest exportRequest = new DipRequest(JsonHandler.getFromString(queryDsql));
        assertThat(client
            .exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), exportRequest)
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }
}
