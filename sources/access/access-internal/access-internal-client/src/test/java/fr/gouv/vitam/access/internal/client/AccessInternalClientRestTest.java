/*
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
package fr.gouv.vitam.access.internal.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessInternalClientRestTest extends ResteasyTestApplication {
    private static final String DUMMY_REQUEST_ID = "reqId";
    private static AccessInternalClientRest client;
    private final static ExpectedResults mock = mock(ExpectedResults.class);

    public static VitamServerTestRunner vitamServerTestRunner =
        new VitamServerTestRunner(AccessInternalClientRestTest.class, AccessInternalClientFactory.getInstance());

    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        client = (AccessInternalClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    final String queryDsl =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    final String emptyQueryDsl =
        "{ \"$query\" : \"\", " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    final String ID = "identfier1";
    final String USAGE = "BinaryMaster";
    final int VERSION = 1;
    final String UNIT_ID = "unitId";


    @Path("/access-internal/v1")
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource implements AccessInternalResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Override
        @GET
        @Path("/units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnits(JsonNode queryDsl) {
            return expectedResponse.post();
        }

        @Override
        @GET
        @Path("/unitsWithInheritedRules")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitsWithInheritedRules(JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @Override
        public Response exportDIP(JsonNode dslRequest) {
            return null;
        }

        @Override
        public Response exportDIPByUsageFilter(DipExportRequest dipExportRequest) {
            return null;
        }

        @Override
        public Response findDIPByID(String id) {
            return null;
        }

        @Override
        @POST
        @Path("/reclassification")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startReclassificationWorkflow(JsonNode reclassificationRequest) {
            return expectedResponse.post();
        }

        @Override
        @GET
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(JsonNode queryDsl,
            @PathParam("id_unit") String id_unit) {
            return expectedResponse.post();
        }

        @GET
        @Path("/objects/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_XML)
        @Override
        public Response getObjectByIdWithXMLFormat(JsonNode dslQuery, @PathParam("id_unit") String objectId) {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_XML)
        public Response getUnitByIdWithXMLFormat(JsonNode dslQuery, @PathParam("id_unit") String unitId) {
            return expectedResponse.get();
        }

        @GET
        @Path("/units/{id_unit}/object")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_XML)
        @Override
        public Response getObjectByUnitIdWithXMLFormat(JsonNode dslQuery, @PathParam("id_unit") String unitId) {
            return expectedResponse.get();
        }

        @Override
        @PUT
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitById(JsonNode queryDsl, @PathParam("id_unit") String id_unit,
            @HeaderParam(GlobalDataRest.X_REQUEST_ID) String requestId) {
            return expectedResponse.put();
        }

        @GET
        @Path("/status")
        @Override
        public Response status() {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroup(
            @PathParam("id_object_group") String idObjectGroup,
            JsonNode query) {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/objects/{id_object_group}/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStreamAsync(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup,
            @PathParam("id_unit") String idUnit) {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/storageaccesslog")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getAccessLogStreamAsync(@Context HttpHeaders headers, JsonNode params) {
            return expectedResponse.get();
        }

        @Override
        @POST
        @Path("/units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response massUpdateUnits(JsonNode queryDsl) {
            return expectedResponse.put();
        }

        /**
         * Mass update of archive units rules
         *
         * @param massUpdateUnitRuleRequest wrapper for {DSL, RuleActions}, null not allowed
         * @return the response
         */
        @Override
        public Response massUpdateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest) {
            return null;
        }

        // Functionalities related to TRACEABILITY operation

        @POST
        @Path("/traceability/check")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkTraceabilityOperation(JsonNode query)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/traceability/{idOperation}/content")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("idOperation") String operationId)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjects(JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @Override
        @POST
        @Path("/elimination/analysis")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startEliminationAnalysisWorkflow(EliminationRequestBody eliminationRequestBody) {
            return expectedResponse.post();
        }

        @Override
        @POST
        @Path("/elimination/action")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startEliminationActionWorkflow(EliminationRequestBody eliminationRequestBody) {
            return expectedResponse.post();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequestException_whenSelect_ThenRaiseAnException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.FORBIDDEN).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(emptyQueryDsl);
        assertThatThrownBy(() -> client.selectUnits(queryJson))
            .isInstanceOf(BadRequestException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenResourceNotFound_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnits(queryJson))
            .isInstanceOf(AccessInternalClientNotFoundException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequest_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnits(queryJson))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    // Select Unit By Id

    @RunWithCustomExecutor
    @Test
    public void givenInternalServerError_whenSelectById_ThenRaiseAnException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnitbyId(queryJson, ID))
            .isInstanceOf(AccessInternalClientServerException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenResourceNotFound_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnitbyId(queryJson, ID))
            .isInstanceOf(AccessInternalClientNotFoundException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequest_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnitbyId(queryJson, ID))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBlankID_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnitbyId(queryJson, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequest_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.updateUnitbyId(queryJson, ID))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenIdBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.updateUnitbyId(queryJson, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequest_whenUpdateUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.updateUnitbyId(queryJson, ID))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void given500_whenUpdateUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.updateUnitbyId(queryJson, ID))
            .isInstanceOf(AccessInternalClientServerException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        assertThatThrownBy(() -> client.selectObjectbyId(null, ID))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectObjectbyId(queryJson, ID))
            .isInstanceOf(AccessInternalClientServerException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectObjectbyId(queryJson, ID))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenRaisePreconditionFailed() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectObjectbyId(queryJson, ID))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectObjectbyId(queryJson, ID))
            .isInstanceOf(AccessInternalClientNotFoundException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("{ \"$hits\": {\"total\":\"1\"} }").build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThat(client.selectObjectbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.getObject(ID, USAGE, VERSION, UNIT_ID))
            .isInstanceOf(AccessInternalClientServerException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.getObject(ID, USAGE, VERSION, UNIT_ID))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaisePreconditionFailed() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatThrownBy(() -> client.getObject(ID, USAGE, VERSION, UNIT_ID))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatThrownBy(() -> client.getObject(ID, USAGE, VERSION, UNIT_ID))
            .isInstanceOf(AccessInternalClientNotFoundException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(StreamUtils.toInputStream("Vitam test")).build());
        final InputStream stream = client.getObject(ID, USAGE, VERSION, UNIT_ID).readEntity(InputStream.class);
        final InputStream stream2 = StreamUtils.toInputStream("Vitam test");
        assertNotNull(stream);
        assertTrue(StreamUtils.contentEquals(stream, stream2));
    }

    @Test
    public void statusExecutionWithoutBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.checkStatus();
    }

    @RunWithCustomExecutor
    @Test
    public void givenCorrectDslQueryWhenCheckTraceabilityOperationThenOK() throws Exception {

        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.ok().entity(ClientMockResultHelper.checkOperationTraceability()).build());

        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        @SuppressWarnings("rawtypes")
        final RequestResponse requestResponse =
            client.checkTraceabilityOperation(queryJson);
        assertNotNull(requestResponse);
        assertTrue(requestResponse.toJsonNode().has("$results"));
    }

    @RunWithCustomExecutor
    @Test
    public void givenOperationIdWhenDownloadTraceabilityOperationThenOK() throws Exception {

        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        Response response = client.downloadTraceabilityFile("OP_ID");
        assertNotNull(response);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequestException_whenSelectUnitsWithInheritedRules_ThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.FORBIDDEN).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(emptyQueryDsl);
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(queryJson))
            .isInstanceOf(BadRequestException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenResourceNotFound_whenSelectUnitsWithInheritedRules_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(queryJson))
            .isInstanceOf(AccessInternalClientNotFoundException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void givenBadRequest_whenSelectUnitsWithInheritedRules_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(queryJson))
            .isInstanceOf(InvalidParseOperationException.class);
    }

    /*
     * Elimination analysis
     */

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenSuccessThenReturnVitamResponseOK()
        throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        RequestResponseOK responseOK = new RequestResponseOK();
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(responseOK).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(eliminationRequestBody);

        // Then
        assertThat(requestResponse.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenServerErrorThenReturnInternalServerErrorVitamResponse() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenResourceNotFoundThenReturnNotFoundVitamResponse()
        throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenBadRequestThenReturnBadRequestVitamResponse()
        throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    /*
     * Elimination action
     */

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenSuccessThenReturnVitamResponseOK()
        throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        RequestResponseOK responseOK = new RequestResponseOK();
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(responseOK).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(eliminationRequestBody);

        // Then
        assertThat(requestResponse.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenServerErrorThenReturnInternalServerErrorVitamResponse() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenResourceNotFoundThenReturnNotFoundVitamResponse()
        throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenBadRequestThenReturnBadRequestVitamResponse()
        throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsl));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(eliminationRequestBody);

        // Then
        assertThat(requestResponse.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }
}
