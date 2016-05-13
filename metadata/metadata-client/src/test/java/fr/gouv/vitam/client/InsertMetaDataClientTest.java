package fr.gouv.vitam.client;

import static org.mockito.Mockito.*;
import java.util.function.Supplier;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class InsertMetaDataClientTest extends JerseyTest {
	private static final String QUERY = "";
	private static final String url = "http://localhost:8082";
	private static final MetaDataClient client = new MetaDataClient(url);

	Supplier<Response> mock;

	@Override
	protected Application configure() {
		enable(TestProperties.LOG_TRAFFIC);
		enable(TestProperties.DUMP_ENTITY);
		forceSet(TestProperties.CONTAINER_PORT, "8082");
		mock = mock(Supplier.class);
		return new ResourceConfig().registerInstances(new MyUnitsResource(mock));
	}

	@Path("/metadata/v1")
	public static class MyUnitsResource {
		private Supplier<Response> expectedResponse;

		public MyUnitsResource(Supplier<Response> expectedResponse) {
			this.expectedResponse = expectedResponse;
		}

		@Path("units")
		@POST
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Response insertUnit(String insertRequest) {
			return expectedResponse.get();
		}
	}

	@Test(expected = MetaDataNotFoundException.class)
	public void givenParentNotFoundRequestWhenInsertThenReturnNotFound() throws Exception {
		when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
		client.insert(QUERY);
	}

	@Test(expected = MetaDataAlreadyExistException.class)
	public void givenUnitAlreadyExistsWhenInsertThenReturnConflict() throws Exception {
		when(mock.get()).thenReturn(Response.status(Status.CONFLICT).build());
		client.insert(QUERY);
	}

	@Test(expected = MetaDataDocumentSizeException.class)
	public void givenEntityTooLargeRequestWhenInsertThenReturnRequestEntityTooLarge() throws Exception {
		when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
		client.insert(QUERY);
	}

	@Test(expected = MetaDataExecutionException.class)
	public void shouldRaiseExceptionWhenExecution() throws Exception {
		when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
		client.insert(QUERY);
	}
    @Test(expected = InvalidParseOperationException.class)
    public void givenInvalidRequestWhenInsertThenReturnBadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.insert(QUERY);
    }	
}
