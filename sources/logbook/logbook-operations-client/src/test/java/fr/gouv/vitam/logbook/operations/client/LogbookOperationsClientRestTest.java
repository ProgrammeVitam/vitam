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
package fr.gouv.vitam.logbook.operations.client;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public class LogbookOperationsClientRestTest extends VitamJerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/logbook/v1";
    protected LogbookOperationsClientRest client;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public LogbookOperationsClientRestTest() {
        super(LogbookOperationsClientFactory.getInstance());
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (LogbookOperationsClientRest) getClient();
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
            resourceConfig.registerInstances(new MockResource(mock));
        }
    }
    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

    @Path("/logbook/v1")
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource {
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
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkUpdateLogbookOperation(String parameters) {
            return expectedResponse.put();
        }

        @POST
        @Path("/operations/traceability")
        @Produces(MediaType.APPLICATION_JSON)
        public Response traceability() {
            return expectedResponse.post();
        }

        @POST
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkCreateLogbookOperation(String parameters) {
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
        @Override
        public Response status() {
            return expectedResponse.get();
        }

    }

    private static final LogbookOperationParameters getComplete() {
        return LogbookParametersFactory.newLogbookOperationParameters(GUIDFactory.newRequestIdGUID(0), "eventType",
            GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST, StatusCode.OK, "outcomeDetailMessage",
            GUIDFactory.newRequestIdGUID(0));
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

    @Test
    public void traceability() throws Exception {
        when(mock.post())
            .thenReturn(
                Response.status(Status.OK).entity(
                    "{" + "    \"_id\" : \"aedqaaaaacaam7mxaa72uakyaznzeoiaaaaq\"," +
                        "    \"evId\" : \"aedqaaaaacaam7mxaa72uakyaznzeoiaaaaq\"," +
                        "    \"evType\" : \"PROCESS_SIP_UNITARY\"," +
                        "    \"evDateTime\" : \"2016-10-27T13:37:05.646\"," + "    \"evDetData\" : null," +
                        "    \"evIdProc\" : \"aedqaaaaacaam7mxaa72uakyaznzeoiaaaaq\"," +
                        "    \"evTypeProc\" : \"INGEST\"," + "    \"outcome\" : \"STARTED\"," +
                        "    \"outDetail\" : null," + "    \"outMessg\" : \"aedqaaaaacaam7mxaa72uakyaznzeoiaaaaq\"," +
                        "    \"agIdApp\" : null," + "    \"agIdAppSession\" : null," +
                        "    \"evIdReq\" : \"aedqaaaaacaam7mxaa72uakyaznzeoiaaaaq\"," + "    \"agIdSubm\" : null," +
                        "    \"agIdOrig\" : null," + "    \"obId\" : null," + "    \"obIdReq\" : null," +
                        "    \"obIdIn\" : null," + "    \"events\" : [ " + "        " + "    ]," +
                        "    \"_tenant\" : 0" + "}")
                    .build());
        client.traceability();
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
        client.checkStatus();
    }

    @Test(expected = VitamApplicationServerException.class)
    public void failStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.GATEWAY_TIMEOUT).build());
        client.checkStatus();
    }

    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("{\"name\":\"logbook\",\"role\":\"myRole\"," +
            "\"pid\":123}")
            .build());
        client.checkStatus();
    }

    @Test
    public void selectExecution() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectOperationbyId("id");
            fail("Should raized an exception");
        } catch (final LogbookClientNotFoundException e) {

        }
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectOperation(JsonHandler.createObjectNode());
            fail("Should raized an exception");
        } catch (final LogbookClientNotFoundException e) {

        }
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectOperationbyId("id");
            fail("Should raized an exception");
        } catch (final LogbookClientException e) {

        }
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectOperation(JsonHandler.createObjectNode());
            fail("Should raized an exception");
        } catch (final LogbookClientException e) {

        }
        final GUID eip = GUIDFactory.newEventGUID(0);
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        client.createDelegate(logbookParameters);
        client.updateDelegate(logbookParameters);
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        client.commitCreateDelegate(eip.getId());

        client.updateDelegate(logbookParameters);
        client.updateDelegate(logbookParameters);
        reset(mock);
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        client.commitUpdateDelegate(eip.getId());

        final List<LogbookOperationParameters> list = new ArrayList<>();
        list.add(logbookParameters);
        list.add(logbookParameters);
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        try {
            client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientAlreadyExistsException e) {}
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        try {
            client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {}
        try {
            client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), null);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {}
        reset(mock);
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientNotFoundException e) {}
        reset(mock);
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        try {
            client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {}
        try {
            client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), null);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {}

    }

    @Test
    public void closeExecution() throws Exception {
        client.close();
    }
}
