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
package fr.gouv.vitam.collect.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateStatus;
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientInvalidRequestException;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientNotFoundException;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static fr.gouv.vitam.common.CommonMediaType.TEXT_CSV;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CollectInternalClientRestTest extends ResteasyTestApplication {

    protected static CollectInternalClientRest client;

    private static final ExpectedResults mock = Mockito.mock(ExpectedResults.class);
    static CollectInternalClientFactory factory = CollectInternalClientFactory.getInstance();
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        CollectInternalClientRestTest.class,
        factory
    );

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        client = (CollectInternalClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Before
    public void setUp() throws Exception {}

    @Test
    public void initProject() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.initProject(new ProjectDto());
        assertThat(response).isNotNull();
    }

    @Test
    public void updateProject() throws Exception {
        Mockito.when(mock.put()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.updateProject(new ProjectDto());
        assertThat(response).isNotNull();
    }

    @Test
    public void getProjectById() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.getProjectById("PROJECT_ID");
        assertThat(response).isNotNull();
    }

    @Test
    public void getTransactionById() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.getTransactionById("TRANSACTION_ID");
        assertThat(response).isNotNull();
    }

    @Test
    public void getTransactionByProjectId() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.getTransactionByProjectId("PROJECT_ID");
        assertThat(response).isNotNull();
    }

    @Test
    public void deleteProjectById() throws Exception {
        Mockito.when(mock.delete()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.deleteProjectById("PROJECT_ID");
        assertThat(response).isNotNull();
    }

    @Test
    public void deleteTransactionById() throws Exception {
        Mockito.when(mock.delete()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.deleteTransactionById("TRANSACTION_ID");
        assertThat(response).isNotNull();
    }

    @Test
    public void getProjects() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.getProjects();
        assertThat(response).isNotNull();
    }

    @Test
    public void uploadZipToTransaction() {
        Mockito.when(mock.post()).thenReturn(Response.ok(new RequestResponseOK<>().addResult("RESULT")).build());
        assertThatCode(
            () -> client.uploadZipToTransaction("TX_ID", new NullInputStream(100), null, null)
        ).doesNotThrowAnyException();
    }

    @Test
    public void uploadZipToTransactionWithAttachement() {
        Mockito.when(mock.post()).thenReturn(Response.ok(new RequestResponseOK<>().addResult("RESULT")).build());
        assertThatCode(
            () -> client.uploadZipToTransaction("TX_ID", new NullInputStream(100), null, "ATTACHEMENT_ID")
        ).doesNotThrowAnyException();
    }

    @Test
    public void uploadZipToProjectOK() throws Exception {
        Mockito.when(mock.post()).thenReturn(Response.ok(new RequestResponseOK<>().addResult("RESULT")).build());
        final String result = client.uploadZipToProject("PR_ID", new NullInputStream(100), null);
        assertThat(result).isEqualTo("RESULT");
    }

    @Test
    public void uploadZipToProjectNotFound() {
        Mockito.when(mock.post()).thenReturn(CollectRequestResponse.toVitamError(NOT_FOUND, "Prb"));
        assertThatThrownBy(() -> client.uploadZipToProject("PR_ID", new NullInputStream(100), null))
            .isExactlyInstanceOf(CollectInternalClientNotFoundException.class)
            .hasMessage("Prb");
    }

    @Test
    public void uploadZipToProjectKo() {
        Mockito.when(mock.post()).thenReturn(CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, "Prb"));
        assertThatThrownBy(() -> client.uploadZipToProject("PR_ID", new NullInputStream(100), null))
            .isExactlyInstanceOf(VitamClientException.class)
            .hasMessage("Prb");
    }

    @Test
    public void bulkAtomicUpdateTransactionUnits() throws Exception {
        RequestResponseOK<BulkAtomicUpdateResult> response = client.bulkAtomicUpdateUnits(
            "transactionId",
            JsonHandler.createObjectNode()
        );
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getStatus()).isEqualTo(BulkAtomicUpdateStatus.OK);
        assertThat(response.getResults().get(0).getUpdatedUnitId()).isEqualTo("unitId");
    }

    @Test
    public void updateUnitsWithMetadataCsv_OK() throws Exception {
        RequestResponse<JsonNode> response = client.updateUnitsWithCsvMetadata(
            "transactionId",
            new ByteArrayInputStream("CSV_REQ".getBytes(StandardCharsets.UTF_8))
        );
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getResults()).isEmpty();
    }

    @Test
    public void updateUnitsWithMetadataCsv_KO() {
        assertThatThrownBy(
            () ->
                client.updateUnitsWithCsvMetadata(
                    "transactionId",
                    new ByteArrayInputStream("CSV_REQ_BAD".getBytes(StandardCharsets.UTF_8))
                )
        )
            .isInstanceOf(CollectInternalClientInvalidRequestException.class)
            .hasMessage("BAD !");
    }

    @Test
    public void updateUnitsWithMetadataJsonl_OK() throws Exception {
        RequestResponse<JsonNode> response = client.updateUnitsWithJsonlMetadata(
            "transactionId",
            new ByteArrayInputStream("JSONL_REQ".getBytes(StandardCharsets.UTF_8))
        );
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getResults()).isEmpty();
    }

    @Test
    public void updateUnitsWithMetadataJsonl_KO() {
        assertThatThrownBy(
            () ->
                client.updateUnitsWithJsonlMetadata(
                    "transactionId",
                    new ByteArrayInputStream("JSONL_REQ_BAD".getBytes(StandardCharsets.UTF_8))
                )
        )
            .isInstanceOf(CollectInternalClientInvalidRequestException.class)
            .hasMessage("BAD !");
    }

    @Path("/collect-internal/v1")
    public static class MockResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("/transactions/{transactionId}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getTransactionById(@PathParam("transactionId") String transactionId) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteTransactionById(@PathParam("transactionId") String transactionId) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}/units")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}/units")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnits(@PathParam("transactionId") String transactionId, JsonNode jsonQuery) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}/close")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response closeTransaction(@PathParam("transactionId") String transactionId) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}/send")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response generateAndSendSip(@PathParam("transactionId") String transactionId) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}/units/metadata/csv")
        @PUT
        @Consumes(TEXT_CSV)
        @Produces(APPLICATION_JSON)
        public Response updateUnitsWithMetadataCsv(
            @PathParam("transactionId") String transactionId,
            InputStream metadataCsvInputStream
        ) throws Exception {
            if (!"CSV_REQ".equals(IOUtils.toString(metadataCsvInputStream, StandardCharsets.UTF_8))) {
                return CollectRequestResponse.toVitamError(BAD_REQUEST, "BAD !");
            }
            return Response.ok(new RequestResponseOK<>()).build();
        }

        @Path("/transactions/{transactionId}/units/metadata/jsonl")
        @PUT
        @Consumes(APPLICATION_OCTET_STREAM)
        @Produces(APPLICATION_JSON)
        public Response updateUnitsWithMetadataJsonl(
            @PathParam("transactionId") String transactionId,
            InputStream metadataJsonlInputStream
        ) throws Exception {
            if (!"JSONL_REQ".equals(IOUtils.toString(metadataJsonlInputStream, StandardCharsets.UTF_8))) {
                return CollectRequestResponse.toVitamError(BAD_REQUEST, "BAD !");
            }
            return Response.ok(new RequestResponseOK<>()).build();
        }

        @Path("/projects")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response initProject(ProjectDto projectDto) {
            return expectedResponse.get();
        }

        @Path("/projects")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getProjects() {
            return expectedResponse.get();
        }

        @Path("/projects")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response searchProject(CriteriaProjectDto criteriaProjectDto) {
            return expectedResponse.get();
        }

        @Path("/projects")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateProject(ProjectDto projectDto) {
            return expectedResponse.get();
        }

        @Path("/projects/{projectId}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getProjectById(@PathParam("projectId") String projectId) {
            return expectedResponse.get();
        }

        @Path("/projects/{projectId}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteProjectById(@PathParam("projectId") String projectId) {
            return expectedResponse.get();
        }

        @Path("/projects/{projectId}/units")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Deprecated
        public Response getUnitsByProjectId(@PathParam("projectId") String projectId, JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @Path("/projects/{projectId}/transactions")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAllTransactions(@PathParam("projectId") String projectId) {
            return expectedResponse.get();
        }

        @Path("/projects/{projectId}/transactions")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response initTransaction(TransactionDto transactionDto, @PathParam("projectId") String projectId) {
            return expectedResponse.get();
        }

        @Path("/units/{unitId}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(@PathParam("unitId") String unitId) {
            return expectedResponse.get();
        }

        @Path("/units/{unitId}/objects/{usage}/{version}")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response uploadObjectGroup(
            @PathParam("unitId") String unitId,
            @PathParam("usage") String usageString,
            @PathParam("version") Integer version,
            ObjectDto objectDto
        ) {
            return expectedResponse.get();
        }

        @Path("/objects/{gotId}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectById(@PathParam("gotId") String gotId) {
            return expectedResponse.get();
        }

        @Path("/units/{unitId}/objects/{usage}/{version}/binary")
        @POST
        @Consumes(APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response upload(
            @PathParam("unitId") String unitId,
            @PathParam("usage") String usageString,
            @PathParam("version") Integer version,
            InputStream uploadedInputStream
        ) {
            return expectedResponse.get();
        }

        @Path("/units/{unitId}/objects/{usage}/{version}/binary")
        @GET
        @Produces(APPLICATION_OCTET_STREAM)
        public Response download(
            @PathParam("unitId") String unitId,
            @PathParam("usage") String usageString,
            @PathParam("version") Integer version
        ) {
            return expectedResponse.get();
        }

        @Path("/transactions/{transactionId}/upload")
        @POST
        @Consumes({ CommonMediaType.ZIP })
        @Produces(APPLICATION_JSON)
        public Response uploadZipToTransaction(
            @PathParam("transactionId") String transactionId,
            InputStream inputStreamObject
        ) {
            return expectedResponse.post();
        }

        @Path("/projects/{projectId}/upload")
        @POST
        @Consumes({ CommonMediaType.ZIP })
        @Produces(APPLICATION_JSON)
        public Response uploadZipToProject(@PathParam("projectId") String projectId, InputStream inputStreamObject) {
            return expectedResponse.post();
        }

        @POST
        @Path("/transactions/{transactionId}/units/bulk")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkAtomicUpdateUnits(
            @PathParam("transactionId") String transactionId,
            JsonNode updateQueriesJson
        ) {
            return Response.accepted(
                new RequestResponseOK<BulkAtomicUpdateResult>()
                    .addResult(new BulkAtomicUpdateResult(BulkAtomicUpdateStatus.OK, "unitId", null))
                    .setHttpCode(202)
            ).build();
        }
    }
}
