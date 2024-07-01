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
package fr.gouv.vitam.metadata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class MetaDataClientRestTest extends ResteasyTestApplication {

    protected static MetaDataClientRest client;
    private static final String QUERY = "QUERY";
    private static final String VALID_QUERY = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    static MetaDataClientFactory factory = MetaDataClientFactory.getInstance();
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        MetaDataClientRestTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (MetaDataClientRest) vitamServerTestRunner.getClient();
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

    @Path("/metadata/v1")
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("units")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnit(String request) {
            return expectedResponse.get();
        }

        @Path("units/bulk")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitBulk(String request) {
            return expectedResponse.get();
        }

        @Path("unitsWithInheritedRules")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitWithInheritedRules(String request) {
            return expectedResponse.get();
        }

        @Path("units/{id_unit}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitById(String selectRequest, @PathParam("id_unit") String unitId) {
            return expectedResponse.get();
        }

        @Path("raw/units/{id_unit}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitByIdRaw(@PathParam("id_unit") String unitId) {
            return expectedResponse.get();
        }

        @Path("units/{id_unit}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitbyId(String updateRequest, @PathParam("id_unit") String unitId) {
            return expectedResponse.put();
        }

        @Path("units/atomicupdatebulk")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response atomicUpdateUnitBulk(String request) {
            return expectedResponse.post();
        }

        @Path("objectgroups")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response insertObjectGroup(String insertRequest) {
            return expectedResponse.post();
        }

        @Path("objectgroups/{id_og}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectObjectGroupById(String selectRequest, @PathParam("id_og") String objectGroupId) {
            return expectedResponse.get();
        }

        @Path("raw/objectgroups/{id_og}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupByIdRaw(@PathParam("id_og") String objectGroupId) {
            return expectedResponse.get();
        }

        @Path("accession-registers/units/{operationId}")
        @Produces(MediaType.APPLICATION_JSON)
        @GET
        public Response selectAccessionRegisterForArchiveUnit(@PathParam("operationId") String operationId) {
            return expectedResponse.get();
        }

        @Path("accession-registers/objects/{operationId}")
        @Produces(MediaType.APPLICATION_JSON)
        @GET
        public Response selectAccessionRegisterForObjectGroup(@PathParam("operationId") String operationId) {
            return expectedResponse.get();
        }

        @POST
        @Path("/reindex")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response launchReindexation(JsonNode options) {
            return expectedResponse.post();
        }

        @POST
        @Path("/alias")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response switchIndexes(JsonNode options) {
            return expectedResponse.post();
        }
    }

    @Test
    public void givenParentNotFoundRequestWhenInsertObjectGroupsThenReturnNotFound() {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatThrownBy(() -> client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY))).isInstanceOf(
            MetaDataNotFoundException.class
        );
    }

    @Test
    public void givenEntityTooLargeRequestWhenInsertObjectGroupsThenReturnRequestEntityTooLarge() {
        when(mock.post()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY))).isInstanceOf(
            MetaDataDocumentSizeException.class
        );
    }

    @Test
    public void givenRequestWhenInsertObjectGroupAndUnavailableServerThenReturnInternaServerError() {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY))).isInstanceOf(
            MetaDataExecutionException.class
        );
    }

    @Test
    public void givenInvalidRequestWhenInsertObjectGroupsThenReturnBadRequest() {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY))).isInstanceOf(
            Exception.class
        );
    }

    @Test
    public void insertObjectGroupTest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(JsonHandler.createObjectNode()).build());
        client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test
    public void selectUnitShouldRaiseExceptionWhenExecution() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.selectUnits(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_InvalidRequest_When_Select_ThenReturn_BadRequest() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectUnits(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_EntityTooLargeRequest_When_select_ThenReturn_RequestEntityTooLarge() {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.selectUnits(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_EntityTooLargeRequest_When_Select_ThenReturn_not_acceptable() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectUnits(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_blankQuery_whenSelectUnit_ThenReturn_MetadataInvalidSelectException() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.selectUnits(JsonHandler.getFromString(""))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_internal_server_error_whenSelectUnitById_ThenReturn_internal_server_error() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId")).isInstanceOf(
            MetaDataExecutionException.class
        );
    }

    @Test
    public void given_blankQuery_whenSelectUnitById_ThenReturn_MetadataInvalidSelectException() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.selectUnitbyId(JsonHandler.getFromString(""), "")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_QueryAndBlankUnitId_whenSelectUnitById_ThenReturn_internal_server_error() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_EntityTooLargeRequest_When_selectUnitById_ThenReturn_RequestEntityTooLarge() {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId")).isInstanceOf(
            MetaDataDocumentSizeException.class
        );
    }

    @Test
    public void given_InvalidRequest_When_SelectBYiD_ThenReturn_BadRequest() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_internal_server_error_whenSelectObjectGroupById_ThenReturn_MetaDataExecutionException() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(
            () -> client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId")
        ).isInstanceOf(MetaDataExecutionException.class);
    }

    @Test
    public void given_blankQuery_whenSelectObjectGroupById_ThenReturn_MetadataInvalidSelectException() {
        assertThatThrownBy(() -> client.selectObjectGrouptbyId(JsonHandler.getFromString(""), "")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_QueryAndBlankUnitId_whenSelectObjectGroupById_ThenReturn_internal_server_error() {
        assertThatThrownBy(
            () -> client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void given_EntityTooLargeRequest_When_selectObjectGroupById_ThenReturn_RequestEntityTooLarge() {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(
            () -> client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId")
        ).isInstanceOf(MetaDataDocumentSizeException.class);
    }

    @Test
    public void given_InvalidRequest_When_SelectObjectGroupById_ThenReturn_BadRequest() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(
            () -> client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void given_InvalidRequest_When_SelectObjectGroupById_ThenReturn_PreconditionFailed() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatThrownBy(
            () -> client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void given_ValidRequest_When_SelectObjectGroupById_ThenReturn_OK() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(JsonHandler.createObjectNode().put("test", true)).build()
        );
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId");
    }

    @Test
    public void selectUnitTest() {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("true").build());
        assertThatThrownBy(() -> client.selectUnits(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void selectUnitByIdTest()
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException, MetaDataClientServerException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("true").build());
        client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "id");
    }

    @Test
    public void selectOGByIdTest()
        throws MetaDataClientServerException, MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException, MetaDataNotFoundException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("true").build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "id");
    }

    @Test
    public void given_blankQuery_whenUpdateUnitById_ThenReturn_MetadataInvalidParseException() {
        when(mock.put()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.updateUnitById(JsonHandler.getFromString(""), "")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_QueryAndBlankUnitId_whenUpdateUnitById_ThenReturn_Exception() {
        when(mock.put()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_EntityTooLargeRequest_When_updateUnitById_ThenReturn_RequestEntityTooLarge() {
        when(mock.put()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "unitId")).isInstanceOf(
            MetaDataDocumentSizeException.class
        );
    }

    @Test
    public void given_InvalidRequest_When_UpdateBYiD_ThenReturn_BadRequest() {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).entity(JsonHandler.createObjectNode()).build());
        assertThatThrownBy(() -> client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "unitId")).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void updateUnitByIdTest()
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.OK).entity("true").build());
        client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "id");
    }

    @Test
    public void given_UnexistingUnit_When_UpdateByiD_ThenReturn_NotFound() {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatThrownBy(() -> client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "unitId")).isInstanceOf(
            MetaDataNotFoundException.class
        );
    }

    @Test
    public void should_validate_accession_register_client_for_unit() throws Exception {
        // Given
        RequestResponseOK<UnitPerOriginatingAgency> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(new UnitPerOriginatingAgency("sp1", 3));
        requestResponseOK.addResult(new UnitPerOriginatingAgency("sp2", 4));

        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(JsonHandler.writeAsString(requestResponseOK)).build()
        );

        // When
        List<UnitPerOriginatingAgency> unitPerOriginatingAgencies = client.selectAccessionRegisterOnUnitByOperationId(
            "122345"
        );

        // Then
        assertThat(unitPerOriginatingAgencies)
            .hasSize(2)
            .extracting("value", "count")
            .contains(tuple("sp1", 3l), tuple("sp2", 4l));
    }

    @Test
    public void should_validate_accession_register_client_for_object_group()
        throws InvalidParseOperationException, MetaDataClientServerException {
        // Given
        RequestResponseOK<ObjectGroupPerOriginatingAgency> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(new ObjectGroupPerOriginatingAgency("op1", "sp1", 3, 2, 2000));
        requestResponseOK.addResult(new ObjectGroupPerOriginatingAgency("op1", "sp2", 4, 1, 3400));

        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(JsonHandler.writeAsString(requestResponseOK)).build()
        );

        // When
        List<ObjectGroupPerOriginatingAgency> unitPerOriginatingAgencies =
            client.selectAccessionRegisterOnObjectByOperationId("122345");

        // Then
        assertThat(unitPerOriginatingAgencies)
            .hasSize(2)
            .extracting("operation", "agency", "numberOfObject", "numberOfGOT", "size")
            .contains(tuple("op1", "sp1", 3L, 2L, 2000L), tuple("op1", "sp2", 4L, 1L, 3400L));
    }

    @Test
    @RunWithCustomExecutor
    public void launchReindexationTest()
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).entity(JsonHandler.createObjectNode()).build());
        JsonNode resp = client.reindex(new IndexParameters());
        assertNotNull(resp);
    }

    @Test
    @RunWithCustomExecutor
    public void switchIndexesTest()
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(JsonHandler.createObjectNode()).build());
        assertNotNull(client.switchIndexes(new SwitchIndexParameters()));
    }

    @Test
    @RunWithCustomExecutor
    public void given_OK_When_getObjectGroupByIdRaw_ThenReturn_ObjectGroup() throws Exception {
        // Given
        InputStream objectGroup = PropertiesUtils.getResourceAsStream("objectGroup_raw.json");
        RequestResponse<JsonNode> requestResponseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(objectGroup))
            .setHttpCode(Status.OK.getStatusCode());
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(requestResponseOK).build());

        // When
        RequestResponse<JsonNode> requestResponse = client.getObjectGroupByIdRaw(
            "aebaaaaaaagwky22aboqialbbrqygmaaaaaq"
        );

        // Then
        assertThat(requestResponse).isNotNull();
        assertThat(requestResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(requestResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) requestResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) requestResponse).getFirstResult().get("_id").asText()).isEqualTo(
            "aebaaaaaaagwky22aboqialbbrqygmaaaaaq"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_NoIdSent_When_getObjectGroupByIdRaw_ThenThrow_IllegalArgumentException() throws Exception {
        // Given
        when(mock.get()).thenThrow(new IllegalArgumentException("test"));
        assertThatCode(() -> {
            // When
            client.getObjectGroupByIdRaw(null);
            // Then
        }).isInstanceOf(VitamClientException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void given_InvalidEntity_When_getObjectGroupByIdRaw_ThenThrow_VitamClientException() throws Exception {
        // Given
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("String").build());

        assertThatCode(() -> {
            // When
            client.getObjectGroupByIdRaw("aebaaaaaaagwky22aboqialbbrqygmaaaaaq");
            // Then
        }).isInstanceOf(VitamClientException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void given_OK_When_getUnitByIdRaw_ThenReturn_Unit() throws Exception {
        // Given
        InputStream unit = PropertiesUtils.getResourceAsStream("unit_raw.json");
        RequestResponse<JsonNode> requestResponseOK = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromInputStream(unit))
            .setHttpCode(Status.OK.getStatusCode());
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(requestResponseOK).build());

        // When
        RequestResponse<JsonNode> requestResponse = client.getUnitByIdRaw("aeaqaaaaaagwky22aboqialbbrqygviaaaaq");

        // Then
        assertThat(requestResponse).isNotNull();
        assertThat(requestResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(requestResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) requestResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) requestResponse).getFirstResult().get("_id").asText()).isEqualTo(
            "aeaqaaaaaagwky22aboqialbbrqygviaaaaq"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void given_NoIdSent_When_getUnitByIdRaw_ThenThrow_IllegalArgumentException() throws Exception {
        // Given
        when(mock.get()).thenThrow(new IllegalArgumentException("test"));
        assertThatCode(() -> {
            // When
            client.getUnitByIdRaw(null);
            // Then
        }).isInstanceOf(VitamClientException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void given_InvalidEntity_When_getUnitByIdRaw_ThenThrow_VitamClientException() throws Exception {
        // Given
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("String").build());

        assertThatCode(() -> {
            // When
            client.getUnitByIdRaw("aeaqaaaaaagwky22aboqialbbrqygviaaaaq");
            // Then
        }).isInstanceOf(VitamClientException.class);
    }

    @Test
    public void selectObjectsShouldRaiseExceptionWhenExecution() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.selectObjectGroups(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void givenInvalidRequestWhenSelectObjectsThenReturnBadRequest() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectObjectGroups(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void givenEntityTooLargeRequestWhenSelectObjectsThenReturnRequestEntityTooLarge() {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.selectObjectGroups(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void givenEntityTooLargeRequestWhenSelectThenReturnNotAcceptable() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectObjectGroups(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void givenBlankQueryWhenSelectObjectsThenReturnMetadataInvalidSelectException() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.selectObjectGroups(JsonHandler.getFromString(""))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void selectObjectsTest() {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("true").build());
        assertThatThrownBy(() -> client.selectObjectGroups(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void selectUnitsWithoutInheritedRulesShouldRaiseExceptionWhenExecution() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_InvalidRequest_When_SelectUnitsWithoutInheritedRules_ThenReturn_BadRequest() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_EntityTooLargeRequest_When_selectUnitsWithoutInheritedRules_ThenReturn_RequestEntityTooLarge() {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_EntityTooLargeRequest_When_SelectUnitsWithoutInheritedRules_ThenReturn_not_acceptable() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void given_blankQuery_whenSelectUnitsWithoutInheritedRules_ThenReturn_MetadataInvalidSelectException() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(""))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void selectUnitsWithoutInheritedRulesTest() {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("true").build());
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(QUERY))).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    public void selectUnitBulkTest_Ok() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        List<RequestResponseOK<JsonNode>> response = Collections.singletonList(
            new RequestResponseOK<JsonNode>().addResult(JsonHandler.createObjectNode())
        );
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(response).build());
        assertThatCode(() -> client.selectUnitsBulk(queries)).doesNotThrowAnyException();
    }

    @Test
    public void selectUnitBulkTest_VitamError() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        VitamError vitamResponse = new VitamError(Status.INTERNAL_SERVER_ERROR.name())
            .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .setContext("METADATA")
            .setState("CODE_VITAM")
            .setMessage(Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .setDescription("Horror exception just because");
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamResponse).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(MetaDataExecutionException.class);
    }

    @Test
    public void selectUnitBulkTest_WrongResponseFormat() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("wrong format").build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void selectUnitBulkTest_NullParam() throws InvalidParseOperationException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<JsonNode>()).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void selectUnitBulkTest_BadRequest() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void selectUnitBulkTest_PreconditionFailed() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void selectUnitBulkTest_InternalError() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(MetaDataExecutionException.class);
    }

    @Test
    public void selectUnitBulkTest_RequestTooLarge() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(MetaDataDocumentSizeException.class);
    }

    @Test
    public void selectUnitBulkTest_NotFound() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatThrownBy(() -> client.selectUnitsBulk(queries)).isInstanceOf(MetaDataClientServerException.class);
    }

    // UpdateUnitBulk

    @Test
    public void atomicUpdateBulkTest_Ok() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<JsonNode>()).build());
        assertThatCode(() -> client.atomicUpdateBulk(queries)).doesNotThrowAnyException();
    }

    @Test
    public void atomicUpdateBulkTest_VitamError() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        VitamError vitamResponse = new VitamError(Status.INTERNAL_SERVER_ERROR.name())
            .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .setContext("METADATA")
            .setState("CODE_VITAM")
            .setMessage(Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .setDescription("Too bad another exception");
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(vitamResponse).build());
        assertThatCode(() -> client.atomicUpdateBulk(queries)).doesNotThrowAnyException();
    }

    @Test
    public void atomicUpdateBulkTest_WrongResponseFormat() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("wrong format").build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(queries)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void atomicUpdateBulkTest_NullParam() throws InvalidParseOperationException {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<JsonNode>()).build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void atomicUpdateBulkTest_BadRequest() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(queries)).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void atomicUpdateBulkTest_PreconditionFailed() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(queries)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void atomicUpdateBulkTest_InternalError() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(queries)).isInstanceOf(MetaDataExecutionException.class);
    }

    @Test
    public void atomicUpdateBulkTest_RequestTooLarge() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(queries)).isInstanceOf(MetaDataDocumentSizeException.class);
    }

    @Test
    public void atomicUpdateBulkTest_NotFound() throws InvalidParseOperationException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatThrownBy(() -> client.atomicUpdateBulk(queries)).isInstanceOf(MetaDataNotFoundException.class);
    }

    @Test
    public void atomicUpdateBulkTest_BlankQuery() {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        when(mock.post()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        assertThatThrownBy(() -> {
            queries.add(JsonHandler.getFromString(""));
            client.atomicUpdateBulk(queries);
        }).isInstanceOf(InvalidParseOperationException.class);
    }
}
