/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.logbook.lifecycles.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import org.apache.commons.io.input.NullInputStream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.GlobalDataRest.X_EVENT_STATUS;
import static fr.gouv.vitam.common.model.LifeCycleStatusCode.LIFE_CYCLE_COMMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LogbookLifeCyclesClientRestTest extends ResteasyTestApplication {

    protected static LogbookLifeCyclesClientRest client;

    protected static final ExpectedResults mock = mock(ExpectedResults.class);
    static LogbookLifeCyclesClientFactory factory = LogbookLifeCyclesClientFactory.getInstance();

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        LogbookLifeCyclesClientRestTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (LogbookLifeCyclesClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Before
    public void before() {
        reset(mock);
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/logbook/v1")
    public static class MockResource {

        private final ExpectedResults mock;

        public MockResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @GET
        @Path("/operations/{id_op}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperationOnlyById(@PathParam("id_op") String id) {
            return mock.get();
        }

        @GET
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperation(@PathParam("id_op") String id, JsonNode queryDsl) {
            return mock.get();
        }

        @POST
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createOperation(@PathParam("id_op") String operationId, LogbookOperationParameters operation) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateOperation(@PathParam("id_op") String operationId, LogbookOperationParameters operation) {
            return mock.put();
        }

        @POST
        @Path("/operations/traceability")
        @Produces(MediaType.APPLICATION_JSON)
        public Response traceability(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
            return mock.post();
        }

        @POST
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkCreateOperation(JsonNode query) {
            return mock.post();
        }

        @GET
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperation(JsonNode query) {
            return mock.get();
        }

        @PUT
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateOperationBulk(String arrayNodeOperations) {
            return mock.put();
        }

        @GET
        @Path("/operations/{id_op}/unitlifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            JsonNode query
        ) {
            return mock.get();
        }

        @POST
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createUnitLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId,
            LogbookLifeCycleUnitParameters parameters
        ) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/unit/temporary")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesUnitTemporaryByOperation(
            @PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
        ) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/got/temporary")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesGOTTemporaryByOperation(
            @PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
        ) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/unit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesUnitByOperation(
            @PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
        ) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/got")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesGOTByOperation(
            @PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
        ) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesUnitTemporaryByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            LogbookLifeCycleUnitParameters parameters
        ) {
            return mock.put();
        }

        @DELETE
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteUnitLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId
        ) {
            return mock.delete();
        }

        @Deprecated
        @PUT
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}/commit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response commitUnitLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId
        ) {
            return mock.put();
        }

        @POST
        @Path("/operations/{id_op}/unitlifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkCreateUnit(@PathParam("id_op") String idOp, String array) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}/lifecycles/objectgroup/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response createLifeCycleObjectGroupBulk(
            @PathParam("id_op") String idOp,
            List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels
        ) {
            return mock.put();
        }

        @PUT
        @Path("/operations/{id_op}/lifecycles/unit/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response createLifeCycleUnitBulk(
            @PathParam("id_op") String idOp,
            List<LogbookLifeCycleUnitModel> logbookLifeCycleModels
        ) {
            return mock.put();
        }

        @PUT
        @Path("/operations/{id_op}/unitlifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateBulkUnit(@PathParam("id_op") String idOp, String arrayNodeLifecycle) {
            return mock.put();
        }

        @GET
        @Path("/unitlifecycles/{id_lc}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCycleById(
            @PathParam("id_lc") String unitLifeCycleId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            JsonNode queryDsl
        ) {
            return mock.get();
        }

        @HEAD
        @Path("/unitlifecycles/{id_lc}")
        public Response getUnitLifeCycleStatus(@PathParam("id_lc") String unitLifeCycleId) {
            return mock.head();
        }

        @GET
        @Path("/unitlifecycles")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCycle(
            JsonNode queryDsl,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus
        ) {
            return mock.get();
        }

        @POST
        @Path("/raw/unitlifecycles/bylastpersisteddate")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawUnitLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
            return mock.post();
        }

        @GET
        @Path("/raw/unitlifecycles/byid/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawUnitLifeCycleById(@PathParam("id") String id) {
            return mock.get();
        }

        @GET
        @Path("/operations/{id_op}/objectgrouplifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            JsonNode query
        ) {
            return mock.get();
        }

        @POST
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createObjectGroupLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId,
            LogbookLifeCycleObjectGroupParameters parameters
        ) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateObjectGroupLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            LogbookLifeCycleObjectGroupParameters parameters
        ) {
            return mock.put();
        }

        @DELETE
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteObjectGroupLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId
        ) {
            return mock.delete();
        }

        @Deprecated
        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}/commit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response commitObjectGroupLifeCyclesByOperation(
            @PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId
        ) {
            return mock.put();
        }

        @POST
        @Path("/operations/{id_op}/objectgrouplifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkCreateObjectGroup(@PathParam("id_op") String idOp, String array) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateBulkObjectGroup(@PathParam("id_op") String idOp, String arrayNodeLifecycle) {
            return mock.put();
        }

        @GET
        @Path("/objectgrouplifecycles/{id_lc}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycleById(
            @PathParam("id_lc") String objectGroupLifeCycleId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            JsonNode queryDsl
        ) {
            return mock.get();
        }

        @GET
        @Path("/objectgrouplifecycles")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycle(
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            JsonNode queryDsl
        ) {
            return mock.get();
        }

        @HEAD
        @Path("/objectgrouplifecycles/{id_lc}")
        public Response getObjectGroupLifeCycleStatus(@PathParam("id_lc") String objectGroupLifeCycleId) {
            return mock.head();
        }

        @GET
        @Path("/status")
        public Response getObjectGroupLifeCycleStatus() {
            return mock.get();
        }

        @POST
        @Path("/raw/objectgrouplifecycles/bylastpersisteddate")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawObjectGroupLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
            return mock.post();
        }

        @GET
        @Path("/raw/objectgrouplifecycles/byid/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawObjectGroupLifeCycleById(@PathParam("id") String id) {
            return mock.get();
        }

        @DELETE
        @Path("/operations/{id_op}/unitlifecycles")
        public Response rollBackUnitLifeCyclesByOperation(@PathParam("id_op") String operationId) {
            return mock.delete();
        }

        @DELETE
        @Path("/operations/{id_op}/objectgrouplifecycles")
        public Response rollBackObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId) {
            return mock.delete();
        }

        @POST
        @Path("/lifecycles/units/traceability")
        @Produces(MediaType.APPLICATION_JSON)
        public Response traceabilityLfcUnit(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
            return mock.post();
        }

        @POST
        @Path("/lifecycles/objectgroups/traceability")
        @Produces(MediaType.APPLICATION_JSON)
        public Response traceabilityLfcObjectGroup(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
            return mock.post();
        }

        @GET
        @Path("/lifecycles/traceability/check/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkLifecycleTraceabilityStatus(@PathParam("id") String operationId) {
            return mock.get();
        }

        @Path("/reindex")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response reindex(IndexParameters indexParameters) {
            return mock.post();
        }

        @Path("/alias")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response changeIndexes(SwitchIndexParameters switchIndexParameters) {
            return mock.post();
        }

        @Path("/auditTraceability")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response launchTraceabilityAudit(AuditLogbookOptions options) {
            return mock.post();
        }

        @DELETE
        @Path("/objectgrouplifecycles/bulkDelete")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteObjectGroups(List<String> objectGroupIds) {
            return mock.delete();
        }

        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/lifeCycleUnits/bulkDelete")
        public Response deleteUnits(List<String> unitsIdentifier) {
            return mock.delete();
        }

        @POST
        @Path("raw/unitlifecycles/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response createLifeCycleUnitBulkRaw(List<JsonNode> logbookLifecycles) {
            return mock.post();
        }

        @POST
        @Path("raw/objectgrouplifecycles/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response createLifeCycleObjectGroupBulkRaw(List<JsonNode> logbookLifecycles) {
            return mock.post();
        }

        @GET
        @Path("/raw/unitlifecycles/byids")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawUnitLifeCycleById(List<String> ids) {
            return mock.get();
        }

        @GET
        @Path("/raw/objectgrouplifecycles/byids")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawObjectGroupLifeCycleById(List<String> ids) {
            return mock.get();
        }
    }

    private static final LogbookLifeCycleUnitParameters getCompleteLifeCycleUnitParameters() {
        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;

        logbookLifeCyclesUnitParametersStart = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier, eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString());

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        return logbookLifeCyclesUnitParametersStart;
    }

    private static final LogbookLifeCycleObjectGroupParameters getCompleteLifeCycleObjectGroupParameters() {
        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParametersStart;

        logbookLifeCycleObjectGroupParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifier,
            eip.toString()
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.objectIdentifier,
            ioL.toString()
        );

        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetail,
            "outcomeDetail"
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        return logbookLifeCycleObjectGroupParametersStart;
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenCreateThenReturnIllegalArgumentExceptionUnitLifeCycle() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.CONFLICT).build());
        final LogbookLifeCycleUnitParameters log = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
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
        assertThatCode(() -> client.create(log)).doesNotThrowAnyException();
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
    public void updateExecutionUnitLifeCycle() {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        assertThatCode(() -> client.update(log)).doesNotThrowAnyException();
    }

    @Test
    public void commitExecutionUnitLifeCycle() {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        assertThatCode(() -> client.commit(log)).doesNotThrowAnyException();
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
    public void rollbacktExecutionUnitLifeCycle() {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        assertThatCode(() -> client.rollback(log)).doesNotThrowAnyException();
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
    public void statusExecutionWithouthBody() {
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        assertThatCode(() -> client.checkStatus()).doesNotThrowAnyException();
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
        final LogbookLifeCycleUnitParameters log = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
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
    public void createExecutionObjectGroupLifeCycle() {
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        final LogbookLifeCycleUnitParameters log = getCompleteLifeCycleUnitParameters();
        assertThatCode(() -> client.create(log)).doesNotThrowAnyException();
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
    public void updateExecutionObjectGroupLifeCycle() {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        assertThatCode(() -> client.update(log)).doesNotThrowAnyException();
    }

    @Test
    public void commitExecutionObjectGroupLifeCycle() {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        assertThatCode(() -> client.commit(log)).doesNotThrowAnyException();
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
        assertThatCode(() -> client.rollback(log)).doesNotThrowAnyException();
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
        when(mock.delete()).thenReturn(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
        final LogbookLifeCycleObjectGroupParameters log = getCompleteLifeCycleObjectGroupParameters();
        client.rollback(log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rollbacktExecutionObjectGroup_ThrowLogbookClientBadRequestException() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParametersStart;

        logbookLifeCycleObjectGroupParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.STARTED);

        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetail,
            "outcomeDetail"
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );

        client.rollback(logbookLifeCycleObjectGroupParametersStart);
    }

    @Test
    public void selectExecution() throws Exception {
        final String BODY_WITHOUT_ID = "{\"$query\": {}, \"$projection\": {}, \"$filter\": {}}";
        final String BODY_WITH_ID =
            "{\"$query\": {\"$eq\": {\"obId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\" }}, \"$projection\": {}, \"$filter\": {}}";

        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectObjectGroupLifeCycleById("id", JsonHandler.getFromString(BODY_WITHOUT_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientNotFoundException e) {}
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectUnitLifeCycleById("id", JsonHandler.getFromString(BODY_WITHOUT_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientNotFoundException e) {}
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectUnitLifeCycle(JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientNotFoundException e) {}
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectObjectGroupLifeCycleById("id", JsonHandler.getFromString(BODY_WITHOUT_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientException e) {}
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectUnitLifeCycleById("id", JsonHandler.getFromString(BODY_WITHOUT_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientException e) {}
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectUnitLifeCycle(JsonHandler.getFromString(BODY_WITH_ID));
            fail("Should raise an exception");
        } catch (final LogbookClientException e) {}
        assertThatThrownBy(
            () -> client.unitLifeCyclesByOperationIterator("id", LIFE_CYCLE_COMMITTED, JsonHandler.createObjectNode())
        ).isInstanceOf(LogbookClientServerException.class);
        assertThatThrownBy(
            () ->
                client.objectGroupLifeCyclesByOperationIterator(
                    "id",
                    LIFE_CYCLE_COMMITTED,
                    JsonHandler.createObjectNode()
                )
        ).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void closeExecution() throws Exception {
        assertThatCode(() -> client.close()).doesNotThrowAnyException();
    }

    @Test
    public void commitUnitThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID unitId = GUIDFactory.newUnitGUID(0);
        assertThatCode(() -> client.commitUnit(operationId.getId(), unitId.getId())).doesNotThrowAnyException();
    }

    @Test
    public void commitObjectGroupThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.put()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGroup = GUIDFactory.newUnitGUID(0);
        assertThatCode(() -> client.commitUnit(operationId.getId(), objectGroup.getId())).doesNotThrowAnyException();
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
        assertThatCode(() -> client.rollBackUnitsByOperation(operationId.getId())).doesNotThrowAnyException();
    }

    @Test
    public void rollBackObjetctGroupsByOperationThenReturnOk()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        when(mock.delete()).thenReturn(Response.status(Response.Status.OK).build());
        GUID operationId = GUIDFactory.newOperationLogbookGUID(0);
        assertThatCode(() -> client.rollBackObjectGroupsByOperation(operationId.getId())).doesNotThrowAnyException();
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
        when(mock.head()).thenReturn(
            Response.status(Response.Status.OK).header(X_EVENT_STATUS, LIFE_CYCLE_COMMITTED).build()
        );

        GUID unitId = GUIDFactory.newUnitGUID(0);
        assertThatCode(() -> client.getUnitLifeCycleStatus(unitId.toString())).doesNotThrowAnyException();
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
        when(mock.head()).thenReturn(
            Response.status(Response.Status.OK).header(X_EVENT_STATUS, LIFE_CYCLE_COMMITTED).build()
        );

        GUID objectGroupId = GUIDFactory.newObjectGroupGUID(0);
        assertThatCode(() -> client.getObjectGroupLifeCycleStatus(objectGroupId.toString())).doesNotThrowAnyException();
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
    public void testRawbulkUnitLifecycles_InternalError() {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkUnitlifecycles(lifecycles);
        }).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void testRawbulkUnitLifecycles_BadRequest() {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkUnitlifecycles(lifecycles);
        }).isInstanceOf(LogbookClientBadRequestException.class);
    }

    @Test
    public void testRawbulkUnitLifecycles_Created() {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());
        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> client.createRawbulkUnitlifecycles(lifecycles)).doesNotThrowAnyException();
    }

    @Test
    public void testRawbulkObjectgroupLifecycles_InternalError() {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkObjectgrouplifecycles(lifecycles);
        }).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void testRawbulkObjectgroupLifecycles_BadRequest() {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkObjectgrouplifecycles(lifecycles);
        }).isInstanceOf(LogbookClientBadRequestException.class);
    }

    @Test
    public void testRawbulkObjectgroupLifecycles_Created() {
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.CREATED).build());

        List<JsonNode> lifecycles = new ArrayList<>();
        assertThatCode(() -> {
            client.createRawbulkObjectgrouplifecycles(lifecycles);
        }).doesNotThrowAnyException();
    }

    @Test
    public void getRawUnitLifecyclesByLastPersistedDate_OK() throws Exception {
        when(mock.post()).thenReturn(
            Response.ok(new NullInputStream(100)).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), "100").build()
        );
        InputStream inputStream = client.exportRawUnitLifecyclesByLastPersistedDate(
            LocalDateUtil.now(),
            LocalDateUtil.now(),
            1000
        );
        assertThat(ByteStreams.toByteArray(inputStream)).hasSize(100);
    }

    @Test
    public void getRawUnitLifecyclesByLastPersistedDateBadSize() throws Exception {
        when(mock.post()).thenReturn(
            Response.ok(new NullInputStream(100)).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), "50").build()
        );
        InputStream inputStream = client.exportRawUnitLifecyclesByLastPersistedDate(
            LocalDateUtil.now(),
            LocalDateUtil.now(),
            1000
        );

        assertThatThrownBy(() -> ByteStreams.toByteArray(inputStream)).isInstanceOf(IOException.class);
    }

    @Test
    public void getRawUnitLifecyclesByLastPersistedDateMissingSize() {
        when(mock.post()).thenReturn(Response.ok(new NullInputStream(100)).build());
        assertThatThrownBy(
            () -> client.exportRawUnitLifecyclesByLastPersistedDate(LocalDateUtil.now(), LocalDateUtil.now(), 1000)
        ).isInstanceOf(LogbookClientException.class);
    }

    @Test
    public void getRawUnitLifecyclesByLastPersistedDate_InternalServerError() {
        when(mock.post()).thenReturn(Response.serverError().build());
        assertThatThrownBy(
            () -> client.exportRawUnitLifecyclesByLastPersistedDate(LocalDateUtil.now(), LocalDateUtil.now(), 1000)
        ).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void getRawObjectGroupLifecyclesByLastPersistedDate_OK() throws Exception {
        when(mock.post()).thenReturn(
            Response.ok(new NullInputStream(100)).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), "100").build()
        );
        InputStream inputStream = client.exportRawObjectGroupLifecyclesByLastPersistedDate(
            LocalDateUtil.now(),
            LocalDateUtil.now(),
            1000
        );
        assertThat(ByteStreams.toByteArray(inputStream)).hasSize(100);
    }

    @Test
    public void getRawObjectGroupLifecyclesByLastPersistedDateBadSize() throws Exception {
        when(mock.post()).thenReturn(
            Response.ok(new NullInputStream(100)).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), "50").build()
        );
        InputStream inputStream = client.exportRawObjectGroupLifecyclesByLastPersistedDate(
            LocalDateUtil.now(),
            LocalDateUtil.now(),
            1000
        );

        assertThatThrownBy(() -> ByteStreams.toByteArray(inputStream)).isInstanceOf(IOException.class);
    }

    @Test
    public void getRawObjectGroupLifecyclesByLastPersistedDateMissingSize() {
        when(mock.post()).thenReturn(Response.ok(new NullInputStream(100)).build());
        assertThatThrownBy(
            () ->
                client.exportRawObjectGroupLifecyclesByLastPersistedDate(LocalDateUtil.now(), LocalDateUtil.now(), 1000)
        ).isInstanceOf(LogbookClientException.class);
    }

    @Test
    public void getRawObjectGroupLifecyclesByLastPersistedDate_InternalServerError() {
        when(mock.post()).thenReturn(Response.serverError().build());
        assertThatThrownBy(
            () ->
                client.exportRawObjectGroupLifecyclesByLastPersistedDate(LocalDateUtil.now(), LocalDateUtil.now(), 1000)
        ).isInstanceOf(LogbookClientServerException.class);
    }

    @Test
    public void getRawUnitLifeCyclesById_OK() throws LogbookClientException, InvalidParseOperationException {
        when(mock.get()).thenReturn(
            new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse()
        );
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
        when(mock.get()).thenReturn(
            new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse()
        );
        client.getRawObjectGroupLifeCycleById("id");
    }

    @Test
    public void getRawObjectGroupLifeCycleById_NotFound() {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertThatThrownBy(() -> {
            client.getRawObjectGroupLifeCycleById("id");
        }).isInstanceOf(LogbookClientNotFoundException.class);
    }

    @Test
    public void getRawUnitLifeCyclesByIds_OK() throws LogbookClientException, InvalidParseOperationException {
        when(mock.get()).thenReturn(
            new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse()
        );
        client.getRawUnitLifeCycleByIds(Arrays.asList("id1", "id2"));
    }

    @Test
    public void getRawUnitLifeCycleByIds_NotFound() {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertThatThrownBy(() -> {
            client.getRawUnitLifeCycleByIds(Arrays.asList("id1", "id2"));
        }).isInstanceOf(LogbookClientNotFoundException.class);
    }

    @Test
    public void getRawObjectGroupLifeCycleByIds_OK() throws LogbookClientException, InvalidParseOperationException {
        when(mock.get()).thenReturn(
            new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.OK.getStatusCode()).toResponse()
        );
        client.getRawObjectGroupLifeCycleByIds(Arrays.asList("id1", "id2"));
    }

    @Test
    public void getRawObjectGroupLifeCycleByIds_NotFound() {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        assertThatThrownBy(() -> {
            client.getRawObjectGroupLifeCycleByIds(Arrays.asList("id1", "id2"));
        }).isInstanceOf(LogbookClientNotFoundException.class);
    }
}
