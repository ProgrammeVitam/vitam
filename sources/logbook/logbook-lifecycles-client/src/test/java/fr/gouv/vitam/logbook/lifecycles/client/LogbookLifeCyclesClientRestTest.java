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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.junit.VitamServerTestRunner;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LogbookLifeCyclesClientRestTest extends ResteasyTestApplication {

    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/logbook/v1";
    protected static LogbookLifeCyclesClientRest client;


    protected static ExpectedResults mock;


    static JunitHelper junitHelper = JunitHelper.getInstance();
    static int serverPortNumber = junitHelper.findAvailablePort();

    static LogbookLifeCyclesClientFactory factory = LogbookLifeCyclesClientFactory.getInstance();
    @ClassRule
    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(LogbookLifeCyclesClientRestTest.class, factory, serverPortNumber);


    @BeforeClass
    public static void init() {
        client = (LogbookLifeCyclesClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        JunitHelper.getInstance().releasePort(serverPortNumber);
        VitamClientFactory.resetConnections();
    }

    @Override
    public Set<Object> getResources() {
        mock = mock(ExpectedResults.class);

        return Sets.newHashSet(new MockResource(mock));
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

        @PUT
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}/commit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response commitUnitLifeCyclesByOperation(LogbookLifeCycleUnitParameters parameters) {
            return expectedResponse.put();
        }

        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}/commit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response commitObjectGroupLifeCyclesByOperation(LogbookLifeCycleObjectGroupParameters parameters) {
            return expectedResponse.put();
        }

        @DELETE
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteUnitLifeCyclesByOperation(LogbookLifeCycleUnitParameters parameters) {
            return expectedResponse.delete();
        }

        @DELETE
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteObjectGroupLifeCyclesByOperation(LogbookLifeCycleObjectGroupParameters parameters) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }

        @DELETE
        @Path("/operations/{id_op}/objectgrouplifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response rollBackObjectGroupsByOperation(String operationId) {
            return expectedResponse.delete();
        }

        @DELETE
        @Path("/operations/{id_op}/unitlifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response rollBackUnitsByOperation(String operationId) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/unitlifecycles/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitLifeCycleById(String unitId) {
            return expectedResponse.get();
        }

        @GET
        @Path("/objectgrouplifecycles/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectObjectGroupLifeCycleById(String objectGroupId) {
            return expectedResponse.get();
        }

        @HEAD
        @Path("/unitlifecycles/{id}")
        public Response getUnitLifeCycleStatus(String unitId) {
            return expectedResponse.head();
        }

        @HEAD
        @Path("/objectgrouplifecycles/{id}")
        public Response getObjectGroupLifeCycleStatus(String unitId) {
            return expectedResponse.head();
        }

        @POST
        @Path("/raw/unitlifecycles/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response rawbulkUnitLifeCycle(List<JsonNode> unitLifecycles) {
            return expectedResponse.post();
        }

        @POST
        @Path("/raw/objectgrouplifecycles/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response rawbulkObjectgroupLifeCycle(List<JsonNode> objectgroupLifecycles) {
            return expectedResponse.post();
        }

        @GET
        @Path("/raw/unitlifecycles/bylastpersisteddate")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawUnitLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
            return expectedResponse.get();
        }

        @GET
        @Path("/raw/objectgrouplifecycles/bylastpersisteddate")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawObjectGroupLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
            return expectedResponse.get();
        }

        @GET
        @Path("/raw/unitlifecycles/byid/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawUnitLifeCycleById(@PathParam("id") String id) {
            return expectedResponse.get();
        }

        @GET
        @Path("/raw/objectgrouplifecycles/byid/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawObjectGroupLifeCycleById(@PathParam("id") String id) {
            return expectedResponse.get();
        }
    }

    private static final LogbookLifeCycleUnitParameters getCompleteLifeCycleUnitParameters() {
        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;


        logbookLifeCyclesUnitParametersStart = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.STARTED);
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
        logbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.STARTED);
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

    @Test
    public void commitExecutionUnitLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void commitExecutionUnitLifeCycle_ThenThrow_LogbookClientBadRequestException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void commitExecutionUnitLifeCycle_ThenThrow_LogbookClientNotFoundException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void commitExecutionUnitLifeCycle_ThenThrow_LogbookClientServerException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.commit(log);
    }

    @Test
    public void rollbacktExecutionUnitLifeCycle() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void rollbacktExecutionUnitLifeCycle_ThenThrow_LogbookClientBadRequestException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void rollbacktExecutionUnitLifeCycle_ThenThrow_LogbookClientNotFoundException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void rollbacktExecutionUnitLifeCycle_ThenThrow_LogbookClientServerException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.rollback(log);
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.checkStatus();
    }

    @Test(expected = VitamApplicationServerException.class)
    public void failStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.GATEWAY_TIMEOUT).build());
        client.checkStatus();
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

    @Test
    public void commitExecutionObjectGroupLifeCycle() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void commitExecutionObjectGroupLifeCycle_ThenThrow_LogbookClientBadRequestException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void commitExecutionObjectGroupLifeCycle_ThenThrow_LogbookClientNotFoundException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.commit(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void commitExecutionObjectGroupLifeCycle_ThenThrow_LogbookClientServerException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.commit(log);
    }

    @Test
    public void rollbacktExecutionObjectGroup() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void rollbacktExecutionObjectGroup_ThenThrow_LogbookClientBadRequestException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void rollbacktExecutionObjectGroup_ThenThrow_LogbookClientNotFoundException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void rollbacktExecutionObjectGroup_ThenThrow_LogbookClientServerException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = LogbookClientServerException.class)
    public void rollbacktExecutionObjectGroup_ThenThrow_VitamClientInternalException() throws Exception {
        when(mock.delete()).thenThrow(VitamClientInternalException.class);
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rollbacktExecutionObjectGroup_ThrowLogbookClientBadRequestException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParametersStart;

        logbookLifeCycleObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.STARTED);

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
    public void selectExecution() throws Exception {
        final String BODY_WITH_ID =
            "{\"$query\": {\"$eq\": {\"obId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\" }}, \"$projection\": {}, \"$filter\": {}}";

        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectObjectGroupLifeCycleById("id", JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientNotFoundException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectUnitLifeCycleById("id", JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientNotFoundException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectUnitLifeCycle(JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientNotFoundException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectObjectGroupLifeCycleById("id", JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectUnitLifeCycleById("id", JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectUnitLifeCycle(JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientException e) {

        }
        assertNotNull(client.unitLifeCyclesByOperationIterator("id", LifeCycleStatusCode.LIFE_CYCLE_COMMITTED,
            JsonHandler.createObjectNode()));
        assertNotNull(client.objectGroupLifeCyclesByOperationIterator("id", LifeCycleStatusCode.LIFE_CYCLE_COMMITTED,
            JsonHandler.createObjectNode()));
    }

    @Test
    public void closeExecution() throws Exception {
        client.close();
    }

    @Test
    public void commitUnitThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID unitId = GUIDFactory.newUnitGUID(0);
        client.commitUnit(operationId.getId(), unitId.getId());
    }

    @Test
    public void commitObjectGroupThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGroup = GUIDFactory.newUnitGUID(0);
        client.commitUnit(operationId.getId(), objectGroup.getId());
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void commitUnit_ThrowLogbookClientBadRequestException()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID unit = GUIDFactory.newUnitGUID(0);
        client.commitUnit(operationId.getId(), unit.getId());
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void commitUnit_ThrowLogbookClientNotFoundException()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID unit = GUIDFactory.newUnitGUID(0);
        client.commitUnit(operationId.getId(), unit.getId());
    }

    @Test(expected = LogbookClientServerException.class)
    public void commitUnit_ThrowLogbookClientServerException()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID unit = GUIDFactory.newUnitGUID(0);
        client.commitUnit(operationId.getId(), unit.getId());
    }

    @Test
    public void rollBackUnitsByOperationThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        client.rollBackUnitsByOperation(operationId.getId());
    }

    @Test
    public void rollBackObjetctGroupsByOperationThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        client.rollBackObjectGroupsByOperation(operationId.getId());
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void rollBackUnitsByOperation__ThrowLogbookClientNotFoundException()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        client.rollBackUnitsByOperation(operationId.getId());
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void rollBackUnitsByOperation__ThrowLogbookClientBadRequestException()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.delete()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        client.rollBackUnitsByOperation(operationId.getId());
    }

    @Test(expected = LogbookClientServerException.class)
    public void rollBackUnitsByOperation__ThrowLogbookClientServerException()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.delete()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        client.rollBackUnitsByOperation(operationId.getId());
    }

    @Test
    public void getUnitLifeCycleStatusThenReturnOk()
        throws LogbookClientNotFoundException, LogbookClientServerException {

        when(mock.head()).thenReturn(Response.status(Response.Status.OK).build());

        GUID unitId = GUIDFactory.newUnitGUID(0);
        client.getUnitLifeCycleStatus(unitId.toString());
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void getUnitLifeCycleStatus_ThrowLogbookClientNotFoundException()
        throws LogbookClientNotFoundException, LogbookClientServerException {

        when(mock.head()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());

        GUID unitId = GUIDFactory.newUnitGUID(0);
        client.getUnitLifeCycleStatus(unitId.toString());
    }

    @Test(expected = LogbookClientServerException.class)
    public void getUnitLifeCycleStatus_ThrowLogbookClientServerException()
        throws LogbookClientNotFoundException, LogbookClientServerException {

        when(mock.head()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        GUID unitId = GUIDFactory.newUnitGUID(0);
        client.getUnitLifeCycleStatus(unitId.toString());
    }

    @Test
    public void getObjectGroupLifeCycleStatusThenReturnOk()
        throws LogbookClientNotFoundException, LogbookClientServerException {

        when(mock.head()).thenReturn(Response.status(Response.Status.OK).build());

        GUID objectGroupId = GUIDFactory.newObjectGroupGUID(0);
        client.getObjectGroupLifeCycleStatus(objectGroupId.toString());
    }

    @Test(expected = LogbookClientNotFoundException.class)
    public void getObjectGroupLifeCycleStatus_ThrowLogbookClientNotFoundException()
        throws LogbookClientNotFoundException, LogbookClientServerException {

        when(mock.head()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());

        GUID objectGroupId = GUIDFactory.newObjectGroupGUID(0);
        client.getObjectGroupLifeCycleStatus(objectGroupId.toString());
    }

    @Test(expected = LogbookClientServerException.class)
    public void getObjectGroupLifeCycleStatus_ThrowLogbookClientServerException()
        throws LogbookClientNotFoundException, LogbookClientServerException {

        when(mock.head()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        GUID objectGroupId = GUIDFactory.newObjectGroupGUID(0);
        client.getObjectGroupLifeCycleStatus(objectGroupId.toString());
    }

    @Test
    public void testRawbulkUnitLifecycles_InternalError()
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkUnitlifecycles(lifecycles);
        }).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void testRawbulkUnitLifecycles_BadRequest()
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkUnitlifecycles(lifecycles);
        }).isInstanceOf(LogbookClientBadRequestException.class);
    }

    @Test
    public void testRawbulkUnitLifecycles_Created()
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkUnitlifecycles(lifecycles);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testRawbulkObjectgroupLifecycles_InternalError()
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkObjectgrouplifecycles(lifecycles);
        }).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void testRawbulkObjectgroupLifecycles_BadRequest()
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkObjectgrouplifecycles(lifecycles);
        }).isInstanceOf(LogbookClientBadRequestException.class);
    }

    @Test
    public void testRawbulkObjectgroupLifecycles_Created()
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkObjectgrouplifecycles(lifecycles);
        }).doesNotThrowAnyException();
    }

    @Test
    public void getRawUnitLifecyclesByLastPersistedDate_OK()
        throws LogbookClientException, InvalidParseOperationException {
        when(mock.get())
            .thenReturn(new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse());
        client.getRawUnitLifecyclesByLastPersistedDate(LocalDateUtil.now(), LocalDateUtil.now(), 1000);
    }

    @Test
    public void getRawObjectGroupLifecyclesByLastPersistedDate_OK()
        throws LogbookClientException, InvalidParseOperationException {
        when(mock.get())
            .thenReturn(new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse());
        client.getRawObjectGroupLifecyclesByLastPersistedDate(LocalDateUtil.now(), LocalDateUtil.now(), 1000);
    }

    @Test
    public void getRawUnitLifeCyclesById_OK() throws LogbookClientException, InvalidParseOperationException {
        when(mock.get())
            .thenReturn(new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse());
        client.getRawUnitLifeCycleById("id");
    }

    @Test
    public void getRawUnitLifeCycleById_NotFound() {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertThatThrownBy(() -> {
            client.getRawUnitLifeCycleById("id");
        }).isInstanceOf(LogbookClientNotFoundException.class);
    }

    @Test
    public void getRawObjectGroupLifeCycleById_OK() throws LogbookClientException, InvalidParseOperationException {
        when(mock.get())
            .thenReturn(new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse());
        client.getRawObjectGroupLifeCycleById("id");
    }

    @Test
    public void getRawObjectGroupLifeCycleById_NotFound() {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertThatThrownBy(() -> {
            client.getRawObjectGroupLifeCycleById("id");
        }).isInstanceOf(LogbookClientNotFoundException.class);
    }
}
