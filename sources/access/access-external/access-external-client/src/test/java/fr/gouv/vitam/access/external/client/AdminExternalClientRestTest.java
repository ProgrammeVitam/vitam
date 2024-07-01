/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.LogbookExternalClientException;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.external.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.access.external.api.AccessExtAPI.RECTIFICATION_AUDIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminExternalClientRestTest extends ResteasyTestApplication {

    private static final String ID = "id";
    private static final String AUDIT_OPTION = "{serviceProducteur: \"Service Producteur 1\"}";
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    final String queryDsql = "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ]}";

    private static final ExpectedResults mock = mock(ExpectedResults.class);

    static AdminExternalClientFactory factory = AdminExternalClientFactory.getInstance();
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        AdminExternalClientRestTest.class,
        factory
    );

    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        AdminExternalClientFactory.changeMode(
            new ClientConfigurationImpl("localhost", vitamServerTestRunner.getBusinessPort())
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/admin-external/v1/")
    public static class MockResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response checkDocument(@PathParam("collections") String collection, InputStream document) {
            return expectedResponse.post();
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importDocument(@PathParam("collections") String collection, InputStream document) {
            return expectedResponse.post();
        }

        @POST
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importDocumentAsJson(@PathParam("collections") String collection, JsonNode document) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("{collections}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteDocuments(@PathParam("collections") String collection) {
            return expectedResponse.delete();
        }

        @GET
        @Path("{collections}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocuments(@PathParam("collections") String collection, JsonNode select) {
            return expectedResponse.get();
        }

        @GET
        @Path("/{collections}/{id_document}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response findDocumentByID(
            @PathParam("collections") String collection,
            @PathParam("id_document") String documentId
        ) {
            return expectedResponse.get();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/" + AccessExtAPI.ACCESSION_REGISTERS_DETAIL)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId, JsonNode select) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/{collections}/{id}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importProfileFile(@PathParam("id") String id, InputStream document) {
            return expectedResponse.put();
        }

        @PUT
        @Path("/{collections}/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateProfile(@PathParam("id") String id, JsonNode queryDsl) {
            return expectedResponse.put();
        }

        @GET
        @Path("/traceability/{id}/datafiles")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("id") String id)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @GET
        @Path("/{collection}/{id}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadFile(@PathParam("id") String id) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path(AccessExtAPI.AUDITS)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkExistenceAudit(JsonNode query) {
            return expectedResponse.post();
        }

        @Path("/operations/{id}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
            return expectedResponse.get();
        }

        @Path("/operations/{id}")
        @HEAD
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
            return expectedResponse.head();
        }

        @Path("operations/{id}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response interruptWorkFlowExecution(@PathParam("id") String id) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
            return expectedResponse.get();
        }

        @Path("operations/{id}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateWorkFlowStatus(@PathParam("id") String id) {
            return expectedResponse.put();
        }

        @GET
        @Path("/rulesreport/{objectId}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadObject(@PathParam("objectId") String objectId) {
            return expectedResponse.get();
        }

        @Path("evidenceaudit")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response evidenceAudit(String query) {
            return expectedResponse.post();
        }

        @Path(RECTIFICATION_AUDIT)
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response rectificationAudit(String query) {
            return expectedResponse.post();
        }

        @Path("probativevalueexport")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response probativeValueExport(String query) {
            return expectedResponse.post();
        }

        @Path(AccessExtAPI.LOGBOOK_OPERATIONS)
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createExternalOperation(JsonNode operation) {
            return expectedResponse.post();
        }
    }

    @Test
    public void testCheckDocument() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream stream = new ByteArrayInputStream("test".getBytes())
        ) {
            Response checkDocumentsResponse = client.checkFormats(new VitamContext(TENANT_ID), stream);
            assertEquals(Status.OK.getStatusCode(), checkDocumentsResponse.getStatus());
        }
    }

    @Test
    public void testCheckDocumentVitamClientException() throws Exception {
        VitamError error = VitamCodeHelper.toVitamError(
            VitamCode.ADMIN_EXTERNAL_CHECK_DOCUMENT_NOT_FOUND,
            "Collection nout found"
        );
        AbstractMockClient.FakeInboundResponse fakeResponse = new AbstractMockClient.FakeInboundResponse(
            Status.NOT_FOUND,
            JsonHandler.writeToInpustream(error),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            new MultivaluedHashMap<>()
        );
        when(mock.post()).thenReturn(fakeResponse);
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream stream = new ByteArrayInputStream("test".getBytes())
        ) {
            Response response = client.checkFormats(new VitamContext(TENANT_ID), stream);
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testCheckDocumentAccessExternalClientException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream stream = new ByteArrayInputStream("test".getBytes())
        ) {
            Response checkDocumentsResponse = client.checkFormats(new VitamContext(TENANT_ID), stream);
            assertEquals(Status.BAD_REQUEST.getStatusCode(), checkDocumentsResponse.getStatus());
        }
    }

    @Test
    public void testImportFormats() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream stream = new ByteArrayInputStream("test".getBytes())
        ) {
            assertEquals(
                client.createFormats(new VitamContext(TENANT_ID), stream, "test.xml").getHttpCode(),
                Status.CREATED.getStatusCode()
            );
        }
    }

    @Test
    public void testImportAgencies() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream stream = new ByteArrayInputStream("test".getBytes())
        ) {
            assertEquals(
                client.createAgencies(new VitamContext(TENANT_ID), stream, "test.csv").getHttpCode(),
                Status.CREATED.getStatusCode()
            );
        }
    }

    @Test
    public void testFindAgencies() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getAgencies()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertEquals(
                client
                    .findAgencies(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                    .toString(),
                ClientMockResultHelper.getAgencies().toString()
            );
        }
    }

    @Test
    public void testAgencyById() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getAgencies()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertEquals(
                client.findAgencyByID(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).toString(),
                ClientMockResultHelper.getAgencies().toString()
            );
        }
    }

    @Test
    public void testImportFormatsAccessExternalClientException() throws Exception {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of formats is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream stream = new ByteArrayInputStream("test".getBytes())
        ) {
            assertEquals(
                Status.BAD_REQUEST.getStatusCode(),
                client.createFormats(new VitamContext(TENANT_ID), stream, "test.xml").getHttpCode()
            );
        }
    }

    @Test
    public void testFindFormats() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormatList()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertEquals(
                client
                    .findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                    .toString(),
                ClientMockResultHelper.getFormatList().toString()
            );
        }
    }

    @Test
    public void testFindFormatsNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                    .getHttpCode()
            ).isEqualTo(Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void testFindFormatsPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                    .getHttpCode()
            ).isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

    @Test
    public void testFindFormatsById() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormat()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertEquals(
                client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).toString(),
                ClientMockResultHelper.getFormat().toString()
            );
        }
    }

    @Test
    public void testFindDocumentByIdAccessExternalClientNotFoundException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).getHttpCode()
            ).isEqualTo(Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void testFindDocumentByIdAccessExternalClientException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).getHttpCode()
            ).isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

    @Test
    public void importContractsWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(new RequestResponseOK<>().addAllResults(getContracts("referential_contracts_ok.json")))
                .build()
        );

        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json")
        ) {
            RequestResponse resp = client.createIngestContracts(new VitamContext(TENANT_ID), fileContracts);
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    private List<Object> getContracts(String filename) throws IOException, InvalidParseOperationException {
        try (InputStream fileContracts = PropertiesUtils.getResourceAsStream(filename)) {
            ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
            List<Object> res = new ArrayList<>();
            array.forEach(res::add);
            return res;
        }
    }

    @Test
    public void importContractsWithIncorrectJsonReturnBadRequest()
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createIngestContracts(new VitamContext(TENANT_ID), new FakeInputStream(0));
            Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (resp.getHttpCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void importContractsWithNullStreamThrowIllegalArgException()
        throws InvalidParseOperationException, AccessExternalClientException {
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.createIngestContracts(new VitamContext(TENANT_ID), null);
        }
    }

    @Test
    public void importAccessContractsWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(new RequestResponseOK<>().addAllResults(getContracts("contracts_access_ok.json")))
                .build()
        );

        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json")
        ) {
            RequestResponse resp = client.createAccessContracts(new VitamContext(TENANT_ID), fileContracts);
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void importAccessContractsWithIncorrectJsonReturnBadRequest()
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createAccessContracts(new VitamContext(TENANT_ID), new FakeInputStream(0));
            Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (resp.getHttpCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void importAccessContractsWithNullStreamThrowIllegalArgException()
        throws InvalidParseOperationException, AccessExternalClientException {
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.createAccessContracts(new VitamContext(TENANT_ID), null);
        }
    }

    @Test
    public void importManagementContractsWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(new RequestResponseOK<>().addAllResults(getContracts("contracts_management_ok.json")))
                .build()
        );

        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_management_ok.json")
        ) {
            RequestResponse resp = client.createManagementContracts(new VitamContext(TENANT_ID), fileContracts);
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void importManagementContractsWithIncorrectJsonReturnBadRequest()
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of contracts is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createManagementContracts(
                new VitamContext(TENANT_ID),
                new FakeInputStream(0)
            );
            Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (resp.getHttpCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void importManagementContractsWithNullStreamThrowIllegalArgException()
        throws InvalidParseOperationException, AccessExternalClientException {
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.createManagementContracts(new VitamContext(TENANT_ID), null);
        }
    }

    /**
     * Test that findAccessContracts is reachable and does not return elements
     */
    @Test
    public void findAllAccessContractsThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .entity(new RequestResponseOK<AccessContractModel>().setHttpCode(Status.OK.getStatusCode()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<AccessContractModel> resp = client.findAccessContracts(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(resp.getStatus()).isEqualTo(Status.OK.getStatusCode());
            assertThat(((RequestResponseOK<AccessContractModel>) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Test that findManagementContracts is reachable and returns elements
     */
    @Test
    public void findAllManagementContractsThenReturnContracts() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getManagementContracts()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ManagementContractModel> resp = client.findManagementContracts(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(resp.getStatus()).isEqualTo(Status.OK.getStatusCode());
            assertThat(((RequestResponseOK<ManagementContractModel>) resp).getResults()).hasSize(1);
        }
    }

    /**
     * Test that findManagementContracts is reachable and returns no elements
     */
    @Test
    public void findNoManagementContractsThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .entity(new RequestResponseOK<ManagementContractModel>().setHttpCode(Status.OK.getStatusCode()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ManagementContractModel> resp = client.findManagementContracts(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(resp.getStatus()).isEqualTo(Status.OK.getStatusCode());
            assertThat(((RequestResponseOK<ManagementContractModel>) resp).getResults()).hasSize(0);
        }
    }

    @Test
    public void findAllFormatsThenReturnOk() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormatList()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<FileFormatModel> resp = client.findFormats(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            RequestResponseOK<FileFormatModel> responseOK = (RequestResponseOK<FileFormatModel>) resp;
            assertThat(responseOK.getResults()).hasSize(1);
            assertThat(responseOK.getFirstResult()).isNotNull();
            assertThat(responseOK.getFirstResult().getPuid()).isEqualTo("x-fmt/20");
        }
    }

    @Test
    public void findAllFormatsThenCollectionNotFound() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<FileFormatModel> response = client.findFormats(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(response.getHttpCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * Test that findAccessContracts is reachable and return two elements as expected
     */
    @Test
    public void findAllAccessContractsThenReturnOne() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getAccessContracts()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<AccessContractModel> resp = client.findAccessContracts(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<AccessContractModel>) resp).getResults()).hasSize(1);
        }
    }

    /**
     * Test that findAccessContractsByID is reachable
     */
    @Test
    public void findAccessContractsByIdThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<AccessContractModel> resp = client.findAccessContractById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId"
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Test that findManagementContractsByID is reachable
     */
    @Test
    public void findManagementContractsByIdThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ManagementContractModel> resp = client.findManagementContractById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId"
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Test that findIngestContracts is reachable and return two elements as expected
     **/
    @Test
    public void findAllIngestContractsThenReturnOne() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getIngestContracts()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<IngestContractModel> resp = client.findIngestContracts(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<IngestContractModel>) resp).getResults()).hasSize(1);
        }
    }

    /**
     * Test that findIngestContractsByID is reachable
     */
    @Test
    public void findIngestContractsByIdThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<IngestContractModel> resp = client.findIngestContractById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId"
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Test that findContexts is reachable and return two elements as expected
     */
    @Test
    public void findAllContextsThenReturnOne() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getContexts(Status.OK.getStatusCode())).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ContextModel> resp = client.findContexts(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<ContextModel>) resp).getResults()).hasSize(1);
        }
    }

    /**
     * Test that findContextsByID is reachable
     */
    @Test
    public void findContextsByIdThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ContextModel> resp = client.findContextById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId"
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    public void createProfilesWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(ClientMockResultHelper.getProfiles(Status.CREATED.getStatusCode()))
                .build()
        );
        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream fileProfiles = PropertiesUtils.getResourceAsStream("contracts_access_ok.json")
        ) {
            RequestResponse resp = client.createProfiles(new VitamContext(TENANT_ID), fileProfiles);
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void createProfilesWithIncorrectJsonReturnBadRequest()
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of profiles is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createProfiles(new VitamContext(TENANT_ID), new FakeInputStream(0));
            Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (resp.getHttpCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void createProfilesWithNullStreamThrowIllegalArgException()
        throws InvalidParseOperationException, AccessExternalClientException {
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.createProfiles(new VitamContext(TENANT_ID), null);
        }
    }

    @Test
    public void importProfileFileXSDReturnCreated()
        throws InvalidParseOperationException, AccessExternalClientException {
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createProfileFile(
                new VitamContext(TENANT_ID),
                "FakeIdXSD",
                new FakeInputStream(0)
            );
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void importProfileFileRNGReturnCreated()
        throws InvalidParseOperationException, AccessExternalClientException {
        when(mock.put()).thenReturn(Response.status(Status.CREATED).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createProfileFile(
                new VitamContext(TENANT_ID),
                "FakeIdRNG",
                new FakeInputStream(0)
            );
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void givenProfileIdWhenDownloadProfileFileThenOK() throws Exception {
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            Response response = client.downloadProfileFile(new VitamContext(TENANT_ID), "OP_ID");
            assertNotNull(response);
        }
    }

    /**
     * Test that findProfiles is reachable and does not return elements
     */
    @Test
    public void findAllProfilesThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ProfileModel> resp = client.findProfiles(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<ProfileModel>) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Test that findProfiles is reachable and return one elements as expected
     */
    @Test
    public void findAllProfilesThenReturnOne() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getProfiles(Status.OK.getStatusCode())).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ProfileModel> resp = client.findProfiles(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<ProfileModel>) resp).getResults()).hasSize(1);
        }
    }

    @Test
    public void findProfilesByIdThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ProfileModel> resp = client.findProfileById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId"
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    public void importContextsWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(new RequestResponseOK<>().addAllResults(getContracts("referential_contracts_ok.json")))
                .build()
        );

        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream fileContexts = PropertiesUtils.getResourceAsStream("contexts_ok.json")
        ) {
            RequestResponse resp = client.createContexts(new VitamContext(TENANT_ID), fileContexts);
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    /**
     * Accession register test
     **/
    @Test
    public void selectAccessionExternalSumary() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getAccessionRegisterSummary()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .findAccessionRegister(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(queryDsql)
                    )
                    .getHttpCode()
            ).isEqualTo(Status.OK.getStatusCode());
        }
    }

    @Test
    public void selectAccessionExternalSumaryError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .findAccessionRegister(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(queryDsql)
                    )
                    .getHttpCode()
            ).isEqualTo(Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void selectAccessionExternalDetail() throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getAccessionRegisterDetail()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .getAccessionRegisterDetail(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        ID,
                        JsonHandler.getFromString(queryDsql)
                    )
                    .getHttpCode()
            ).isEqualTo(Status.OK.getStatusCode());
        }
    }

    @Test
    public void selectAccessionExternalDetailError() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .getAccessionRegisterDetail(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        ID,
                        JsonHandler.getFromString(queryDsql)
                    )
                    .getHttpCode()
            ).isEqualTo(Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * TRACEABILITY operation test
     **/
    @Test
    public void testCheckTraceabilityOperation() throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getLogbooksRequestResponseJsonNode()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.checkTraceabilityOperation(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql)
            );
        }
    }

    @Test
    public void testDownloadTraceabilityOperationFile() throws Exception {
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThatCode(
                () ->
                    client.downloadTraceabilityOperationFile(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        ID
                    )
            ).doesNotThrowAnyException();
        }
    }

    @Test
    public void testCheckExistenceAudit() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        JsonNode auditOption = JsonHandler.getFromString(AUDIT_OPTION);
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client.launchAudit(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), auditOption).getHttpCode()
            ).isEqualTo(Status.OK.getStatusCode());
        }
    }

    @Test
    public void testImportSecurityProfiles() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertEquals(
                client
                    .createSecurityProfiles(
                        new VitamContext(TENANT_ID),
                        new ByteArrayInputStream("{}".getBytes()),
                        "test.json"
                    )
                    .getHttpCode(),
                Status.CREATED.getStatusCode()
            );
        }
    }

    @Test
    public void testFindSecurityProfiles() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getSecurityProfiles()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client.findSecurityProfileById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).isOk()
            ).isTrue();
        }
    }

    @Test
    public void testFindSecurityProfileById() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getSecurityProfiles()).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client.findSecurityProfileById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID).isOk()
            ).isTrue();
        }
    }

    @Test
    public void testUpdateProfile() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .updateProfile(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        ID,
                        JsonHandler.createObjectNode()
                    )
                    .isOk()
            ).isTrue();
        }
    }

    @Test
    public void listOperationsDetailsTest() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .entity(new RequestResponseOK<>().addResult(new ProcessDetail()).setHttpCode(Status.OK.getStatusCode()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ProcessDetail> resp = client.listOperationsDetails(new VitamContext(0), new ProcessQuery());
            assertEquals(resp.getStatus(), Status.OK.getStatusCode());

            when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
            RequestResponse<ProcessDetail> resp2 = client.listOperationsDetails(
                new VitamContext(0),
                new ProcessQuery()
            );
            assertEquals(resp2.getStatus(), Status.INTERNAL_SERVER_ERROR.getStatusCode());

            when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
            RequestResponse<ProcessDetail> resp3 = client.listOperationsDetails(
                new VitamContext(0),
                mock(ProcessQuery.class)
            );
            assertEquals(resp3.getStatus(), Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
        }
    }

    @Test
    public void givenOKWhenGetOperationDetailThenReturnOK() throws VitamClientException, IllegalArgumentException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK.getStatusCode())
                .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> result = client.getOperationProcessExecutionDetails(
                new VitamContext(TENANT_ID),
                ID
            );
            assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
        }
    }

    @Test
    public void givenNotFoundWhenGetOperationDetailThenReturnNotFound()
        throws VitamClientException, IllegalArgumentException {
        when(mock.get()).thenReturn(
            Response.status(Status.NOT_FOUND.getStatusCode())
                .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> result = client.getOperationProcessExecutionDetails(
                new VitamContext(TENANT_ID),
                ID
            );
            assertEquals(result.getHttpCode(), Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void givenOKWhenCancelOperationThenReturnOK() throws VitamClientException, IllegalArgumentException {
        when(mock.delete()).thenReturn(
            Response.status(Status.OK.getStatusCode())
                .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> result = client.cancelOperationProcessExecution(
                new VitamContext(TENANT_ID),
                ID
            );
            assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
        }
    }

    @Test
    public void givenHeadOperationStatusThenOK() throws Exception {
        when(mock.head()).thenReturn(
            Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
                .header(GlobalDataRest.X_CONTEXT_ID, "Fake")
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> resp = client.getOperationProcessStatus(new VitamContext(0), ID);
            assertTrue(resp.isOk());
            ItemStatus itemStatus = ((RequestResponseOK<ItemStatus>) resp).getResults().get(0);
            assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
            assertEquals(Status.OK, itemStatus.getGlobalStatus().getEquivalentHttpStatus());
            assertEquals(ProcessState.COMPLETED, itemStatus.getGlobalState());
        }
    }

    @Test
    public void cancelOperationTest() throws Exception {
        when(mock.delete()).thenReturn(
            Response.status(Status.OK)
                .entity(new RequestResponseOK<>().addResult(new ItemStatus()).setHttpCode(Status.OK.getStatusCode()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
            assertTrue(resp.isOk());
            assertEquals(resp.getStatus(), Status.OK.getStatusCode());

            when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
            resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
            assertFalse(resp.isOk());
            assertEquals(resp.getStatus(), Status.PRECONDITION_FAILED.getStatusCode());

            when(mock.delete()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
            resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
            assertFalse(resp.isOk());
            assertEquals(resp.getStatus(), Status.UNAUTHORIZED.getStatusCode());

            when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
            resp = client.cancelOperationProcessExecution(new VitamContext(0), ID);
            assertFalse(resp.isOk());
            assertEquals(resp.getStatus(), Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void updateOperationActionProcessTest() throws Exception {
        when(mock.put()).thenReturn(
            Response.status(Status.OK)
                .entity(new RequestResponseOK<>().addResult(new ItemStatus()).setHttpCode(Status.OK.getStatusCode()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> resp = client.updateOperationActionProcess(new VitamContext(0), "NEXT", ID);
            assertTrue(resp.isOk());
            assertEquals(Status.OK.getStatusCode(), resp.getStatus());

            when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
            RequestResponse<ItemStatus> resp2 = client.updateOperationActionProcess(new VitamContext(0), "NEXT", ID);
            assertFalse(resp2.isOk());
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp2.getStatus());
        }
    }

    @Test
    public void givenNotFoundWhenDownloadObjectThenReturn404()
        throws VitamClientException, InvalidParseOperationException {
        VitamError error = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, "NOT FOUND");
        AbstractMockClient.FakeInboundResponse fakeResponse = new AbstractMockClient.FakeInboundResponse(
            Status.NOT_FOUND,
            JsonHandler.writeToInpustream(error),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            new MultivaluedHashMap<>()
        );
        when(mock.get()).thenReturn(fakeResponse);
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            InputStream input = client
                .downloadRulesReport(new VitamContext(TENANT_ID), "1")
                .readEntity(InputStream.class);
            VitamError response = JsonHandler.getFromInputStream(input, VitamError.class);
            StreamUtils.closeSilently(input);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getHttpCode());
        }
    }

    @Test
    public void givenInputstreamWhenDownloadObjectThenReturnOK() throws VitamClientException {
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            final InputStream fakeUploadResponseInputStream = client
                .downloadRulesReport(new VitamContext(TENANT_ID), "1")
                .readEntity(InputStream.class);
            assertNotNull(fakeUploadResponseInputStream);

            try {
                assertTrue(
                    IOUtils.contentEquals(
                        fakeUploadResponseInputStream,
                        IOUtils.toInputStream("test", CharsetUtils.UTF_8)
                    )
                );
            } catch (final IOException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    @Test
    public void testCreateArchiveUnitProfilesWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(ClientMockResultHelper.getArchiveUnitProfiles(Status.CREATED.getStatusCode()))
                .build()
        );

        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream fileArchiveUnitProfiles = PropertiesUtils.getResourceAsStream("archive_unit_profile_ok.json")
        ) {
            RequestResponse resp = client.createArchiveUnitProfile(
                new VitamContext(TENANT_ID),
                fileArchiveUnitProfiles
            );
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void testCreateArchiveUnitProfilesWithIncorrectJsonReturnBadRequest()
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of archive unit profiles is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createArchiveUnitProfile(new VitamContext(TENANT_ID), new FakeInputStream(0));
            Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (resp.getHttpCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateArchiveUnitProfilesWithNullStreamThrowIllegalArgException()
        throws InvalidParseOperationException, AccessExternalClientException {
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.createArchiveUnitProfile(new VitamContext(TENANT_ID), null);
        }
    }

    /**
     * Test that findArchiveUnitProfiles is reachable and does not return elements
     */
    @Test
    public void testFindAllArchiveUnitProfilesThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ArchiveUnitProfileModel> resp = client.findArchiveUnitProfiles(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<ArchiveUnitProfileModel>) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Test that findArchiveUnitProfiles is reachable and return one elements as expected
     */
    @Test
    public void testFindAllArchiveUnitProfilesThenReturnOne() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK)
                .entity(ClientMockResultHelper.getArchiveUnitProfiles(Status.OK.getStatusCode()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ArchiveUnitProfileModel> resp = client.findArchiveUnitProfiles(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<ArchiveUnitProfileModel>) resp).getResults()).hasSize(1);
        }
    }

    @Test
    public void testFindArchiveUnitProfilesByIdThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ArchiveUnitProfileModel> resp = client.findArchiveUnitProfileById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                "fakeId"
            );
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    public void testUpdateArchiveUnitProfile() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            assertThat(
                client
                    .updateArchiveUnitProfile(
                        new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        ID,
                        JsonHandler.createObjectNode()
                    )
                    .isOk()
            ).isTrue();
        }
    }

    @Test
    public void evidenceAuditTest() throws VitamClientException, InvalidParseOperationException {
        JsonNode dsl = JsonHandler.getFromString(queryDsql);
        when(mock.post()).thenReturn(
            Response.status(Status.OK.getStatusCode())
                .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> result = client.evidenceAudit(new VitamContext(TENANT_ID), dsl);
            assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
        }
    }

    @Test
    public void rectificationAuditTest() throws VitamClientException {
        String operationId = "id";
        when(mock.post()).thenReturn(
            Response.status(Status.OK.getStatusCode())
                .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> result = client.rectificationAudit(new VitamContext(TENANT_ID), operationId);
            assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
        }
    }

    @Test
    public void probativeValueExportTest() throws VitamClientException, InvalidParseOperationException {
        JsonNode dsl = JsonHandler.getFromString(queryDsql);
        when(mock.post()).thenReturn(
            Response.status(Status.OK.getStatusCode())
                .entity(new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()))
                .build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ItemStatus> result = client.exportProbativeValue(
                new VitamContext(TENANT_ID),
                new ProbativeValueRequest(dsl, "BinaryMaster", "1")
            );
            assertEquals(result.getHttpCode(), Status.OK.getStatusCode());
        }
    }

    @Test
    public void testCreateOntologiesWithCorrectJsonReturnCreated()
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        when(mock.post()).thenReturn(
            Response.status(Status.CREATED)
                .entity(ClientMockResultHelper.getOntologies(Status.CREATED.getStatusCode()))
                .build()
        );

        try (
            AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient();
            InputStream ontologiesFile = PropertiesUtils.getResourceAsStream("ontology_ok.json")
        ) {
            RequestResponse resp = client.importOntologies(true, new VitamContext(TENANT_ID), ontologiesFile);
            Assert.assertTrue(RequestResponseOK.class.isAssignableFrom(resp.getClass()));
            Assert.assertTrue((resp.isOk()));
        }
    }

    @Test
    public void testCreateOntologiesWithIncorrectJsonReturnBadRequest()
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamError error = new VitamError("vitam_code")
            .setHttpCode(400)
            .setContext("ADMIN")
            .setState("INVALID")
            .setMessage("invalid input")
            .setDescription("Input file of ontologies is malformed");
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity(error).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.importOntologies(true, new VitamContext(TENANT_ID), new FakeInputStream(0));
            Assert.assertTrue(VitamError.class.isAssignableFrom(resp.getClass()));
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), (resp.getHttpCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOntologiesWithNullStreamThrowIllegalArgException()
        throws InvalidParseOperationException, AccessExternalClientException {
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            client.importOntologies(true, new VitamContext(TENANT_ID), null);
        }
    }

    /**
     * Test that findOntologies is reachable and return one elements as expected
     */
    @Test
    public void testFindAllOntologiesThenReturnOne() throws VitamClientException {
        when(mock.get()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getOntologies(Status.OK.getStatusCode())).build()
        );
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<OntologyModel> resp = client.findOntologies(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<OntologyModel>) resp).getResults()).hasSize(1);
        }
    }

    /**
     * Test that findOntologies is reachable and does not return elements
     */
    @Test
    public void testFindAllOntologiesThenReturnEmpty() throws VitamClientException {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<>()).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<OntologyModel> resp = client.findOntologies(
                new VitamContext(TENANT_ID).setAccessContract(null),
                JsonHandler.createObjectNode()
            );
            assertThat(resp.isOk()).isTrue();
            assertThat(((RequestResponseOK<OntologyModel>) resp).getResults()).hasSize(0);
        }
    }

    /**
     * Testing creation of external operations
     *
     * @throws LogbookExternalClientException
     */
    @Test
    public void createExternalOperations() throws VitamClientException, LogbookExternalClientException {
        LogbookOperationParameters logbook = LogbookParametersFactory.newLogbookOperationParameters();

        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createExternalOperation(
                new VitamContext(TENANT_ID).setAccessContract(null),
                logbook
            );
            assertEquals(resp.getHttpCode(), Status.CREATED.getStatusCode());
        }

        when(mock.post()).thenReturn(Response.status(Status.CONFLICT).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createExternalOperation(
                new VitamContext(TENANT_ID).setAccessContract(null),
                logbook
            );
            assertEquals(resp.getHttpCode(), Status.CONFLICT.getStatusCode());
        }

        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createExternalOperation(
                new VitamContext(TENANT_ID).setAccessContract(null),
                logbook
            );
            assertEquals(resp.getHttpCode(), Status.BAD_REQUEST.getStatusCode());
        }

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        try (AdminExternalClientRest client = (AdminExternalClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createExternalOperation(
                new VitamContext(TENANT_ID).setAccessContract(null),
                logbook
            );
            assertEquals(resp.getHttpCode(), Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }
}
