/**
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
 */
package fr.gouv.vitam.logbook.operations.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.logbook.common.client.StatusMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import static org.junit.Assert.assertEquals;

public class LogbookOperationsClientRestTest extends JerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final int PORT = 8092;
    protected static final String PATH = "/logbook/v1";
    protected final LogbookOperationsClientRest client;

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response post();

        Response put();

        Response delete();

        Response head();

        Response get();
    }

    public LogbookOperationsClientRestTest() {
        client = new LogbookOperationsClientRest(HOSTNAME, PORT);
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, Integer.toString(PORT));
        mock = mock(ExpectedResults.class);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        return resourceConfig.registerInstances(new MockResource(mock));
    }

    @Path("/logbook/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createLogbookOperation(LogbookOperationParameters parameters) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateLogbookOperation(LogbookOperationParameters parameters) {
            return expectedResponse.put();
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }

    }

    private static final LogbookOperationParameters getComplete() {
        return LogbookParametersFactory.newLogbookOperationParameters("eventIdentifier",
            "eventType", "eventIdentifierProcess", LogbookTypeProcess.INGEST, LogbookOutcome.STARTED,
            "outcomeDetailMessage", "eventIdentifierRequest");
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenCreateThenReturnIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        final LogbookOperationParameters log = LogbookParametersFactory.newLogbookOperationParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientAlreadyExistsException.class)
    public void givenOperationAlreadyCreatedWhenCreateThenReturnAlreadyExistException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        final LogbookOperationParameters log = getComplete();
        client.create(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void shouldRaiseInternalErrorWhenCreateExecution() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final LogbookOperationParameters log = getComplete();
        client.create(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void shouldRaiseBadRequestErrorWhenCreateExecution() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final LogbookOperationParameters log = getComplete();
        client.create(log);
    }

    @Test
    public void createExecution() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        final LogbookOperationParameters log = getComplete();
        client.create(log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenUpdateThenReturnIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        final LogbookOperationParameters log = LogbookParametersFactory.newLogbookOperationParameters();
        client.update(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenOperationNotYetCreatedWhenUpdateThenReturnNotFoundException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final LogbookOperationParameters log = getComplete();
        client.update(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void shouldRaiseInternalErrorWhenUpdateExecution() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final LogbookOperationParameters log = getComplete();
        client.update(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void shouldRaiseBadRequestErrorWhenUpdateExecution() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final LogbookOperationParameters log = getComplete();
        client.update(log);
    }

    @Test
    public void updateExecution() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.OK).build());
        final LogbookOperationParameters log = getComplete();
        client.update(log);
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        client.status();
    }

    @Test(expected = LogbookClientServerException.class)
    public void failStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.GATEWAY_TIMEOUT).build());
        client.status();
    }

    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("{\"name\":\"logbook\",\"role\":\"myRole\"," +
            "\"pid\":123}")
            .build());
        StatusMessage message = client.status();
        assertEquals("logbook", message.getName());
        assertEquals("myRole", message.getRole());
        assertEquals(123, message.getPid());
    }

    @Test
    public void closeExecution() throws Exception {
        client.close();
    }
}
