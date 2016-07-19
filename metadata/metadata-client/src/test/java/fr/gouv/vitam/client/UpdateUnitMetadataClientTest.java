package fr.gouv.vitam.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.junit.JunitHelper;

public class UpdateUnitMetadataClientTest extends JerseyTest {

    private static final String HOST = "http://localhost:";
    private static String url;
    private static MetaDataClient client;
    private static JunitHelper junitHelper;
    private static int port;

    private static final String QUERY_ID = "{$query: {$eq: {\"data\" : \"data2\" }}, $projection: {}, $filter: {}}";

    Supplier<Response> mock;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        url = HOST + port;
        client = new MetaDataClient(url);
    }

    @AfterClass
    public static void shutdownAfterClass() {
        junitHelper.releasePort(port);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, Integer.toString(port));
        mock = mock(Supplier.class);
        return new ResourceConfig().registerInstances(new MyUnitsResource(mock));
    }

    @Path("/metadata/v1")
    public static class MyUnitsResource {
        private final Supplier<Response> expectedResponse;

        public MyUnitsResource(Supplier<Response> expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("units/{id_unit}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitbyId(String insertRequest) {
            return expectedResponse.get();
        }
    }

    @Test(expected = Exception.class)
    public void given_internal_server_error_whenUpdateUnitById_ThenReturnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.updateUnitbyId(QUERY_ID, "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_blankQuery_whenUpdateUnitById_ThenReturn_MetadataInvalidParseException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.updateUnitbyId("", "");
    }

    @Ignore
    @Test(expected = Exception.class)
    public void given_QueryAndBlankUnitId_whenUpdateUnitById_ThenReturn_Exception() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.updateUnitbyId(QUERY_ID, "");
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_EntityTooLargeRequest_When_updateUnitById_ThenReturn_RequestEntityTooLarge() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.updateUnitbyId(QUERY_ID, "unitId");
    }


    @Test(expected = InvalidParseOperationException.class)
    public void given_InvalidRequest_When_UpdateBYiD_ThenReturn_BadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.updateUnitbyId(QUERY_ID, "unitId");
    }
}
