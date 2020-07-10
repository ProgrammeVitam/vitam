/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.logbook.operations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
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
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class LogbookOperationsClientRestTest extends ResteasyTestApplication {
    protected static LogbookOperationsClientRest client;

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    protected final static ExpectedResults mock = mock(ExpectedResults.class);

    static LogbookOperationsClientFactory factory = LogbookOperationsClientFactory.getInstance();
    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(LogbookOperationsClientRestTest.class, factory);

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (LogbookOperationsClientRest) vitamServerTestRunner.getClient();
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
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource {
        private final ExpectedResults mock;

        public MockResource(ExpectedResults expectedResponse) {
            this.mock = expectedResponse;
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
        public Response createOperation(@PathParam("id_op") String operationId,
            LogbookOperationParameters operation) {
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
        public Response getUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode query) {
            return mock.get();
        }

        @POST
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId, LogbookLifeCycleUnitParameters parameters) {
            return mock.post();

        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/unit/temporary")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesUnitTemporaryByOperation(@PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/got/temporary")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesGOTTemporaryByOperation(@PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/unit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesUnitByOperation(@PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {
            return mock.post();
        }

        @POST
        @Path("/operations/{id_op}/bulklifecycles/got")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesGOTByOperation(@PathParam("id_op") String operationId,
            List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitLifeCyclesUnitTemporaryByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId, @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            LogbookLifeCycleUnitParameters parameters) {
            return mock.put();
        }

        @DELETE
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId) {
            return mock.delete();
        }


        @Deprecated
        @PUT
        @Path("/operations/{id_op}/unitlifecycles/{id_lc}/commit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response commitUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String unitLcId) {
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
        public Response createLifeCycleObjectGroupBulk(@PathParam("id_op") String idOp,
            List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels) {
            return mock.put();
        }

        @PUT
        @Path("/operations/{id_op}/lifecycles/unit/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response createLifeCycleUnitBulk(@PathParam("id_op") String idOp,
            List<LogbookLifeCycleUnitModel> logbookLifeCycleModels) {
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
        public Response getUnitLifeCycleById(@PathParam("id_lc") String unitLifeCycleId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode queryDsl) {
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
        public Response getUnitLifeCycle(JsonNode queryDsl,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus) {
            return mock.get();
        }

        @GET
        @Path("/raw/unitlifecycles/bylastpersisteddate")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawUnitLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
            return mock.get();
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
        public Response getObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode query) {
            return mock.get();
        }

        @POST
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId, LogbookLifeCycleObjectGroupParameters parameters) {
            return mock.post();
        }

        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            LogbookLifeCycleObjectGroupParameters parameters) {
            return mock.put();
        }

        @DELETE
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId) {
            return mock.delete();
        }

        @Deprecated
        @PUT
        @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}/commit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response commitObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
            @PathParam("id_lc") String objGrpId) {
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
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        @Override
        public Response status() {
            return mock.get();
        }

        @GET
        @Path("/objectgrouplifecycles/{id_lc}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycleById(@PathParam("id_lc") String objectGroupLifeCycleId,
            @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode queryDsl) {
            return mock.get();
        }

        @GET
        @Path("/objectgrouplifecycles")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycle(@HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
            JsonNode queryDsl) {
            return mock.get();
        }

        @HEAD
        @Path("/objectgrouplifecycles/{id_lc}")
        public Response getObjectGroupLifeCycleStatus(@PathParam("id_lc") String objectGroupLifeCycleId) {
            return mock.head();
        }

        @GET
        @Path("/raw/objectgrouplifecycles/bylastpersisteddate")
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response getRawObjectGroupLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
            return mock.get();
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

    }

    private static final LogbookOperationParameters getComplete() {
        return LogbookParameterHelper.newLogbookOperationParameters(GUIDFactory.newRequestIdGUID(0), "eventType",
            GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST, StatusCode.OK, "outcomeDetailMessage",
            GUIDFactory.newRequestIdGUID(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenCreateThenReturnIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        final LogbookOperationParameters log = LogbookParameterHelper.newLogbookOperationParameters();
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
        VitamThreadUtils.getVitamSession().setTenantId(0);
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
                        "    \"agIdApp\" : null," + "    \"evIdAppSession\" : null," +
                        "    \"evIdReq\" : \"aedqaaaaacaam7mxaa72uakyaznzeoiaaaaq\"," +
                        "    \"agIdExt\" : null," + "    \"obId\" : null," + "    \"obIdReq\" : null," +
                        "    \"obIdIn\" : null," + "    \"events\" : [ " + "        " + "    ]," +
                        "    \"_tenant\" : 0" + "}")
                    .build());
        client.traceability();
    }

    @Test
    public void traceabilityLfcUnit() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(
                new RequestResponseOK<String>().addResult("guid1")).build());
        client.traceabilityLfcObjectGroup();
    }

    @Test
    public void traceabilityLfcObjectGroup() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(
                new RequestResponseOK<String>().addResult("guid1")).build());
        client.traceabilityLfcObjectGroup();
    }

    @Test
    public void checkLifecycleTraceabilityWorkflowStatus() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(
                new RequestResponseOK<LifecycleTraceabilityStatus>().addResult(
                    new LifecycleTraceabilityStatus(true, "MY_STATUS", true)
                )).build());
        LifecycleTraceabilityStatus lifecycleTraceabilityStatus = client.checkLifecycleTraceabilityWorkflowStatus("id");
        assertTrue(lifecycleTraceabilityStatus.isCompleted());
        assertEquals(lifecycleTraceabilityStatus.getOutcome(), "MY_STATUS");
        assertTrue(lifecycleTraceabilityStatus.isMaxEntriesReached());
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumentWhenUpdateThenReturnIllegalArgumentException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        final LogbookOperationParameters log = LogbookParameterHelper.newLogbookOperationParameters();
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
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.selectOperationById("id");
            fail("Should raized an exception");
        } catch (final LogbookClientNotFoundException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectOperationById("id");
            fail("Should raized an exception");
        } catch (final LogbookClientException e) {

        }
        reset(mock);
        when(mock.get()).thenReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
        try {
            client.selectOperation(JsonHandler.createObjectNode());
            fail("Should raized an exception");
        } catch (final LogbookClientException e) {

        }
        final GUID eip = GUIDFactory.newEventGUID(0);
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
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
        } catch (final LogbookClientAlreadyExistsException e) {
        }
        reset(mock);
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        try {
            client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {
        }
        try {
            client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), null);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {
        }
        reset(mock);
        when(mock.put()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        try {
            client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientNotFoundException e) {
        }
        reset(mock);
        when(mock.put()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        try {
            client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), list);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {
        }
        try {
            client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), null);
            fail("Should raized an exception");
        } catch (final LogbookClientBadRequestException e) {
        }

    }


    @Test
    @RunWithCustomExecutor
    public void launchReindexationTest()
        throws InvalidParseOperationException, LogbookClientServerException {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(JsonHandler.createObjectNode())
            .build());
        assertNotNull(client.reindex(new IndexParameters()));
    }

    @Test
    @RunWithCustomExecutor
    public void switchIndexesTest()
        throws InvalidParseOperationException, LogbookClientServerException {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(JsonHandler.createObjectNode())
            .build());
        assertNotNull(client.switchIndexes(new SwitchIndexParameters()));
    }

    @Test
    @RunWithCustomExecutor
    public void traceabilityAuditTest()
        throws InvalidParseOperationException, LogbookClientServerException {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(JsonHandler.createObjectNode())
            .build());
        client.traceabilityAudit(0, new AuditLogbookOptions());
    }

    @Test
    public void closeExecution() throws Exception {
        client.close();
    }
}
