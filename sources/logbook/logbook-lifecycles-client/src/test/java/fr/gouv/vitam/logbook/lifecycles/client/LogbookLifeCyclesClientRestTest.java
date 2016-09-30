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
package fr.gouv.vitam.logbook.lifecycles.client;

import static org.junit.Assert.assertEquals;
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

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Ignore;
import org.junit.Test;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.logbook.common.client.StatusMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public class LogbookLifeCyclesClientRestTest extends JerseyTest {

    protected static final String HOSTNAME = "localhost";
    protected static final int PORT = 8092;
    protected static final String PATH = "/logbook/v1";
    protected final LogbookLifeCyclesClientRest client;

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response post();

        Response put();

        Response delete();

        Response head();

        Response get();
    }

    public LogbookLifeCyclesClientRestTest() {
        client = new LogbookLifeCyclesClientRest(HOSTNAME, PORT);
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
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createLogbookOperation(LogbookOperationParameters parameters) {
            return expectedResponse.post();
        }


        @POST
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createLogbookLifecycleObjectGroupParameters(LogbookLifeCycleObjectGroupParameters parameters) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateLogbookLifecycleUnitParameters(LogbookLifeCycleUnitParameters parameters) {
            return expectedResponse.put();
        }

        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateLogbookLifecycleObjectGroupParameters(LogbookLifeCycleObjectGroupParameters parameters) {
            return expectedResponse.put();
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }
    }

    private static final LogbookLifeCycleUnitParameters getCompleteLifeCycleUnitParameters() {
        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;


        logbookLifeCyclesUnitParametersStart = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(LogbookOutcome.STARTED);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        return logbookLifeCyclesUnitParametersStart;

    }

    private static final LogbookLifeCycleObjectGroupParameters getCompleteLifeCycleObjectGroupParameters() {
        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParametersStart;


        logbookLifeCycleObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParametersStart.setStatus(LogbookOutcome.STARTED);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());

        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        return logbookLifeCycleObjectGroupParametersStart;

    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenCreateThenReturnIllegalArgumentExceptionUnitLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        final LogbookLifeCycleUnitParameters log = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientAlreadyExistsException.class)
    public void givenOperationAlreadyCreatedWhenCreateThenReturnAlreadyExistExceptionUnitLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void shouldRaiseInternalErrorWhenCreateExecutionUnitLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void shouldRaiseBadRequestErrorWhenCreateExecutionUnitLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test
    public void createExecution() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenOperationNotYetCreatedWhenUpdateThenReturnNotFoundExceptionUnitLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void shouldRaiseInternalErrorWhenUpdateExecutionUnitLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void shouldRaiseBadRequestErrorWhenUpdateExecutionUnitLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    @Test
    public void updateExecutionUnitLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    // TODO
    @Ignore
    @Test
    public void commitExecutionUnitLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.commit(log);
    }

    // TODO
    @Ignore
    @Test
    public void rollbacktExecutionUnitLifeCycle() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.rollback(log);
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.status();
    }

    @Test(expected = LogbookClientServerException.class)
    public void failStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.GATEWAY_TIMEOUT).build());
        client.status();
    }

    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get())
            .thenReturn(Response.status(Response.Status.OK).entity("{\"name\":\"logbook\",\"role\":\"myRole\"," +
                "\"pid\":123}")
                .build());
        final StatusMessage message = client.status();
        assertEquals("logbook", message.getName());
        assertEquals("myRole", message.getRole());
        assertEquals(123, message.getPid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenCreateThenReturnIllegalArgumentExceptionObjectGroupLifeCycle()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        final LogbookLifeCycleUnitParameters log = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientAlreadyExistsException.class)
    public void givenOperationAlreadyCreatedWhenCreateThenReturnAlreadyExistExceptionObjectGroupLifeCycle()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void shouldRaiseInternalErrorWhenCreateExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void shouldRaiseBadRequestErrorWhenCreateExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test
    public void createExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.create(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void givenOperationNotYetCreatedWhenUpdateThenReturnNotFoundExceptionObjectGroupLifeCycle()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void shouldRaiseInternalErrorWhenUpdateExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void shouldRaiseBadRequestErrorWhenUpdateExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.update(log);
    }

    @Test
    public void updateExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.update(log);
    }


    @Test(expected = LogbookClientNotFoundException.class)
    public void selectLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.OK).build());
        String id = "ushshsjskqlqlqlqqmqm";
        client.selectLifeCycles(id);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void selectLifeCycleById() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.OK).build());
        String id = "ushshsjskqlqlqlqqmqm";
        client.selectLifeCyclesById(id);
    }


    @Ignore
    @Test
    public void commitExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void commitExecutionObjectGroupLifeCycle_ThrowLogBookNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.commit(log);
    }

    @Ignore
    @Test
    public void rollbacktExecutionObjectGroup() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void rollbacktExecutionObjectGroup_ThrowLogbookClientServerException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rollbacktExecutionObjectGroup_ThrowLogbookClientBadRequestException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParametersStart;

        logbookLifeCycleObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParametersStart.setStatus(LogbookOutcome.STARTED);

        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());

        client.rollback(logbookLifeCycleObjectGroupParametersStart);
    }

    @Test
    public void closeExecution() throws Exception {
        client.close();
    }
}
