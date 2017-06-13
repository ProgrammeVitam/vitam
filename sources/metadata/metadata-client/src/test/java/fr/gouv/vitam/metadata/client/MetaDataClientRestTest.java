/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 **/

package fr.gouv.vitam.metadata.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.List;

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

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

public class MetaDataClientRestTest extends VitamJerseyTest {
    protected MetaDataClientRest client;
    private static final String QUERY = "QUERY";
    private static final String VALID_QUERY = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";

    public MetaDataClientRestTest() {
        super(MetaDataClientFactory.getInstance());
    }

    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (MetaDataClientRest) getClient();
    }

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

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }


    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
    }


    @Path("/metadata/v1")
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("units")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response insertUnit(String request) {
            return expectedResponse.post();
        }

        @Path("units")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnit(String request) {
            return expectedResponse.get();
        }

        @Path("units/{id_unit}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitById(String selectRequest, @PathParam("id_unit") String unitId) {
            return expectedResponse.get();
        }

        @Path("units/{id_unit}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitbyId(String updateRequest, @PathParam("id_unit") String unitId) {
            return expectedResponse.put();
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

        @Path("accession-register/unit/{operationId}")
        @Produces(MediaType.APPLICATION_JSON)
        @GET
        public Response selectAccessionRegisterForArchiveUnit(@PathParam("operationId") String operationId) {
            return expectedResponse.get();
        }
    }

    @Test(expected = MetaDataNotFoundException.class)
    public void givenParentNotFoundRequestWhenInsertThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.insertUnit(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenUnitAlreadyExistsWhenInsertThenReturnConflict() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        client.insertUnit(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenEntityTooLargeRequestWhenInsertThenReturnRequestEntityTooLarge() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.insertUnit(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void shouldRaiseExceptionWhenExecution() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.insertUnit(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInvalidRequestWhenInsertThenReturnBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.insertUnit(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_emptyRequest_When_Insert_ThenReturn_BadRequest() throws Exception {
        client.insertUnit(JsonHandler.getFromString(""));
    }

    @Test
    public void insertUnitTest() throws Exception {
        when(mock.post())
            .thenReturn(Response.status(Status.CREATED).entity(JsonHandler.createObjectNode()).build());
        client.insertUnit(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataNotFoundException.class)
    public void givenParentNotFoundRequestWhenInsertObjectGroupsThenReturnNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenUnitAlreadyExistsWhenInsertObjectGroupsThenReturnConflict() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenEntityTooLargeRequestWhenInsertObjectGroupsThenReturnRequestEntityTooLarge() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenRequestWhenInsertObjectGroupAndUnavailableServerThenReturnInternaServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInvalidRequestWhenInsertObjectGroupsThenReturnBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
    }

    @Test
    public void insertObjectGroupTest() {
        when(mock.post())
            .thenReturn(Response.status(Status.CREATED).entity(JsonHandler.createObjectNode()).build());
        try {
            client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY));
        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException |
            MetaDataClientServerException | MetaDataNotFoundException | MetaDataAlreadyExistException e) {
            fail("Should NOT raized an exception");
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void selectUnitShouldRaiseExceptionWhenExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.selectUnits(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_InvalidRequest_When_Select_ThenReturn_BadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectUnits(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_EntityTooLargeRequest_When_select_ThenReturn_RequestEntityTooLarge() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.selectUnits(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_EntityTooLargeRequest_When_Select_ThenReturn_not_acceptable() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectUnits(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_blankQuery_whenSelectUnit_ThenReturn_MetadataInvalidSelectException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.selectUnits(JsonHandler.getFromString(""));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_internal_server_error_whenSelectUnitById_ThenReturn_internal_server_error() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_blankQuery_whenSelectUnitById_ThenReturn_MetadataInvalidSelectException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.selectUnitbyId(JsonHandler.getFromString(""), "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_QueryAndBlankUnitId_whenSelectUnitById_ThenReturn_internal_server_error() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "");
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_EntityTooLargeRequest_When_selectUnitById_ThenReturn_RequestEntityTooLarge() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId");
    }


    @Test(expected = InvalidParseOperationException.class)
    public void given_InvalidRequest_When_SelectBYiD_ThenReturn_BadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId");
    }


    @Test(expected = MetaDataExecutionException.class)
    public void given_internal_server_error_whenSelectObjectGroupById_ThenReturn_MetaDataExecutionException()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_blankQuery_whenSelectObjectGroupById_ThenReturn_MetadataInvalidSelectException()
        throws Exception {
        client.selectObjectGrouptbyId(JsonHandler.getFromString(""), "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_QueryAndBlankUnitId_whenSelectObjectGroupById_ThenReturn_internal_server_error()
        throws Exception {
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "");
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_EntityTooLargeRequest_When_selectObjectGroupById_ThenReturn_RequestEntityTooLarge()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_InvalidRequest_When_SelectObjectGroupById_ThenReturn_BadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId");
    }

    @Test(expected = MetadataInvalidSelectException.class)
    public void given_InvalidRequest_When_SelectObjectGroupById_ThenReturn_PreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId");
    }

    @Test
    public void given_ValidRequest_When_SelectObjectGroupById_ThenReturn_OK() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "ogId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void selectUnitTest()
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException,
        MetaDataClientServerException {
        when(mock.get()).thenReturn(Response.status(Status.FOUND).entity("true").build());
        client.selectUnits(JsonHandler.getFromString(QUERY));
    }

    @Test
    public void selectUnitByIdTest()
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException,
        MetaDataClientServerException {
        when(mock.get()).thenReturn(Response.status(Status.FOUND).entity("true").build());
        client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "id");
    }

    @Test
    public void selectOGByIdTest()
        throws MetaDataClientServerException, MetaDataDocumentSizeException, MetaDataExecutionException,
        InvalidParseOperationException, MetadataInvalidSelectException {
        when(mock.get()).thenReturn(Response.status(Status.FOUND).entity("true").build());
        client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "id");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_blankQuery_whenUpdateUnitById_ThenReturn_MetadataInvalidParseException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.updateUnitbyId(JsonHandler.getFromString(""), "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_QueryAndBlankUnitId_whenUpdateUnitById_ThenReturn_Exception() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        client.updateUnitbyId(JsonHandler.getFromString(VALID_QUERY), "");
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_EntityTooLargeRequest_When_updateUnitById_ThenReturn_RequestEntityTooLarge() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build());
        client.updateUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_InvalidRequest_When_UpdateBYiD_ThenReturn_BadRequest() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.updateUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId");
    }

    @Test
    public void updateUnitByIdTest()
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException,
        MetaDataClientServerException {
        when(mock.put()).thenReturn(Response.status(Status.FOUND).entity("true").build());
        client.updateUnitbyId(JsonHandler.getFromString(VALID_QUERY), "id");
    }


    @Test
    public void should_validate_accesion_register_client()
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException,
        MetaDataClientServerException {

        // Given
        RequestResponseOK<UnitPerOriginatingAgency> requestResponseOK =
            new RequestResponseOK<>();
        requestResponseOK.addResult(new UnitPerOriginatingAgency("sp1", 3));
        requestResponseOK.addResult(new UnitPerOriginatingAgency("sp2", 4));

        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(JsonHandler.writeAsString(requestResponseOK)).build());

        // When
        List<UnitPerOriginatingAgency> unitPerOriginatingAgencies =
            client.selectAccessionRegisterByOperationId("122345");

        // Then
        assertThat(unitPerOriginatingAgencies).hasSize(2).extracting("id", "count")
            .contains(tuple("sp1", 3), tuple("sp2", 4));
    }

}

