package fr.gouv.vitam.ingest.external.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;

public class IngestExternalClientRestTest extends JerseyTest{
    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/ingest-ext/v1";
    protected final IngestExternalClientRest client;
    private static JunitHelper junitHelper;
    private static int port;
    
    protected ExpectedResults mock;

    interface ExpectedResults {
        Response post();
        Response get();
    }
    
    public IngestExternalClientRestTest() {
        client = new IngestExternalClientRest(HOSTNAME, port);
    }
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        junitHelper.releasePort(port);
    }
    
    @Override
    protected Application configure() {
        // enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, Integer.toString(port));
        mock = mock(ExpectedResults.class);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        return resourceConfig.registerInstances(new MockResource(mock));
    }

    @Path("/ingest-ext/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("upload")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response upload(InputStream stream) {
            return expectedResponse.post();
        }

        @GET
        @Path("status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }

    }
    
    @Test
    public void givenStatusOK() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        client.status();
    }
    
    @Test
    public void givenInputstreamWhenUploadThenReturnOK() throws IngestExternalException{
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("no-virus.txt");
        client.upload(stream);
    }
    
    @Test(expected = IngestExternalException.class)
    public void givenOperationNotYetCreatedWhenUpdateThenReturnNotFoundException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED).build());
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("unfixed-virus.txt");
        client.upload(stream);
    }
    
}
