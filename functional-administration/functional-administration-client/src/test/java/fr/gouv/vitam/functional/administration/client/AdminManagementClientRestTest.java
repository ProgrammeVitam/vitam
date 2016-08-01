package fr.gouv.vitam.functional.administration.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

public class AdminManagementClientRestTest extends JerseyTest {

    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/adminmanagement/v1";
    
    protected AdminManagementClientRest client = null;
    
    private static JunitHelper junitHelper;
    private static int port;

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response post();
        Response checkFormat();

        Response delete();

        Response get();
        
        Response getDocument();        
    }

    public AdminManagementClientRestTest() {
        client = new AdminManagementClientRest(HOSTNAME, port);
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
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, Integer.toString(port));
        mock = mock(ExpectedResults.class);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        return resourceConfig.registerInstances(new MockResource(mock));
    }    
    
    @Path("/adminmanagement/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("/format/check")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkFormat(InputStream xmlPronom) {
            return expectedResponse.checkFormat();
        }
        
        @POST
        @Path("/format/import")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importFormat(InputStream xmlPronom) {
            return expectedResponse.post();
        }        

        @DELETE
        @Path("/format/delete")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteFormat() {
            return expectedResponse.post();
        }        
        
        @POST
        @Path("/format/{id_format}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findFileFormatByID() {
            return expectedResponse.post();
        }

        @GET
        @Path("/format/document")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocument() {
            return expectedResponse.get();
        }
        
        @GET
        @Path("/status")
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
    public void givenInputstreamOKWhenCheckThenReturnOK() throws ReferentialException{
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        client.checkFormat(stream);
    }
    
    @Test(expected=ReferentialException.class)
    public void givenInputstreamKOWhenCheckThenReturnKO() throws Exception {
        when(mock.checkFormat()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam-format-KO.xml");        
        client.checkFormat(stream);    
        System.out.println("Test not OK");        
    }
    
    
    @Test
    public void givenInputstreamOKWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);
    }
    
    
    @Test
    public void whenDeteleFormatReturnKO() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.OK).build());
        client.deleteFormat();
    }
        
    @Ignore
    @Test    
    public void givenAnInvalidQueryThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());        
        Select select = new Select();        
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);
        when(mock.getDocument()).thenReturn(Response.status(Status.OK).build());
        JsonNode jsonDocument = client.getFormats(select.getFinalSelect());  
    }    
}
