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
package fr.gouv.vitam.access.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.access.api.AccessResource;
import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

public class AccessClientRestTest extends JerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final int PORT = 8082;
    protected static final String PATH = "/access/v1";
    protected final AccessClientRest client;

    final String queryDsql =
        "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
            " $filter : { $orderby : { '#id' } }," +
            " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
            " }";
    final String ID = "identfier1";
    final String USAGE = "BinaryMaster";
    final int VERSION = 1;

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response post();

        Response get();

        Response put();
    }

    public AccessClientRestTest() {
        client = new AccessClientRest(HOSTNAME, PORT);
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

    @Path("/access/v1")
    public static class MockResource implements AccessResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Override
        @POST
        @Path("/units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnits(String queryDsl,
            @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
            return expectedResponse.post();
        }

        @Override
        @POST
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(String queryDsl,
            @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
            @PathParam("id_unit") String id_unit) {
            return expectedResponse.post();
        }

        @Override
        @PUT
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitById(String queryDsl, @PathParam("id_unit") String id_unit) {
            return expectedResponse.put();
        }

        @GET
        @Path("/status")
        public Response getStatus() {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }

        @Override
        @POST
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroup(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xHttpOverride,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
        }

        @Override
        @GET
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStream(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }

        @Override
        @POST
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStreamPost(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
        }

    }

    @Test(expected = AccessClientServerException.class)
    public void givenInternalServerError_whenSelect_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = AccessClientNotFoundException.class)
    public void givenRessourceNotFound_whenSelectUnit_ThenRaiseAnException()
        throws AccessClientNotFoundException, AccessClientServerException, InvalidParseOperationException {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenSelectUnit_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.selectUnits(queryDsql)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenRequestBlank_whenSelectUnit_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnits("")).isNotNull();

    }
    // Select Unit By Id


    @Test(expected = AccessClientServerException.class)
    public void givenInternalServerError_whenSelectById_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = AccessClientNotFoundException.class)
    public void givenRessourceNotFound_whenSelectUnitById_ThenRaiseAnException()
        throws AccessClientNotFoundException, AccessClientServerException, InvalidParseOperationException {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";

        assertThat(client.selectUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenSelectUnitById_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectUnitbyId(queryDsql, ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenRequestBlank_whenSelectUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnitbyId("", "")).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIDBlank_whenSelectUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnitbyId(queryDsql, "")).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenrEQUESTBlank_IDFilledwhenSelectUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.selectUnitbyId("", ID)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenUpdateUnitById_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.updateUnitbyId(queryDsql, ID)).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenRequestBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.updateUnitbyId("", "")).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenIdBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.updateUnitbyId(queryDsql, "")).isNotNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void givenrEquestBlank_IDFilledwhenUpdateUnitById_ThenRaiseAnException()
        throws IllegalArgumentException, AccessClientServerException, AccessClientNotFoundException,
        InvalidParseOperationException {
        assertThat(client.updateUnitbyId("", ID)).isNotNull();
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenUpdateUnit_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.updateUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = AccessClientServerException.class)
    public void given500_whenUpdateUnit_ThenRaiseAnException()
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        assertThat(client.updateUnitbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnIllegalArgumentException() throws Exception {
        client.selectObjectbyId(null, ID);
    }

    @Test(expected = AccessClientServerException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.selectObjectbyId(queryDsql, ID);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.selectObjectbyId(queryDsql, ID);
    }

    @Test(expected = AccessClientServerException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaisePreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.selectObjectbyId(queryDsql, ID);
    }

    @Test(expected = AccessClientNotFoundException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.selectObjectbyId(queryDsql, ID);
    }

    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{ \"hint\": {\"total\":\"1\"} }").build());
        assertThat(client.selectObjectbyId(queryDsql, ID)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenQueryNullWhenGetObjectAsInputStreamThenRaiseAnIllegalArgumentException() throws Exception {
        client.getObjectAsInputStream(null, ID, USAGE, VERSION);
    }

    @Test(expected = AccessClientServerException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseInternalServerError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getObjectAsInputStream(queryDsql, ID, USAGE, VERSION);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.getObjectAsInputStream(queryDsql, ID, USAGE, VERSION);
    }

    @Test(expected = AccessClientServerException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaisePreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getObjectAsInputStream(queryDsql, ID, USAGE, VERSION);
    }

    @Test(expected = AccessClientNotFoundException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getObjectAsInputStream(queryDsql, ID, USAGE, VERSION);
    }

    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(IOUtils.toInputStream("Vitam test")).build());
        final InputStream stream = client.getObjectAsInputStream(queryDsql, ID, USAGE, VERSION);
        final InputStream stream2 = IOUtils.toInputStream("Vitam test");
        assertNotNull(stream);
        assertTrue(IOUtils.contentEquals(stream, stream2));
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.status();
    }

}
