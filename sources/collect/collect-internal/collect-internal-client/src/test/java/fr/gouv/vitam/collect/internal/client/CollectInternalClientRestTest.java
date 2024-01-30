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
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class CollectInternalClientRestTest extends ResteasyTestApplication {

    protected static CollectInternalClientRest client;

    private final static ExpectedResults mock = Mockito.mock(ExpectedResults.class);
    static CollectInternalClientFactory factory = CollectInternalClientFactory.getInstance();
    public static VitamServerTestRunner vitamServerTestRunner =
        new VitamServerTestRunner(CollectInternalClientRestTest.class, factory);

    @Rule public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

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
    public void setUp() throws Exception {
    }

    @Test
    public void initProject() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.initProject(new ProjectDto());
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void updateProject() throws Exception {
        Mockito.when(mock.put()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.updateProject(new ProjectDto());
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void getProjectById() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.getProjectById("PROJECT_ID");
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void getTransactionById() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response =
            client.getTransactionById("TRANSACTION_ID");
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void getTransactionByProjectId() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response =
            client.getTransactionByProjectId("PROJECT_ID");
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void deleteProjectById() throws Exception {
        Mockito.when(mock.delete()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.deleteProjectById("PROJECT_ID");
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void deleteTransactionById() throws Exception {
        Mockito.when(mock.delete()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response =
            client.deleteTransactionById("TRANSACTION_ID");
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void getProjects() throws Exception {
        Mockito.when(mock.get()).thenReturn(Response.ok().build());
        final RequestResponse<JsonNode> response = client.getProjects();
        Assertions.assertThat(response).isNotNull();
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

        @Path("/transactions/{transactionId}/units")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnits(@PathParam("transactionId") String transactionId, InputStream is) {
            return expectedResponse.get();
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

        @Path("/transactions/{transactionId}/upload")
        @POST
        @Consumes({CommonMediaType.ZIP})
        @Produces(APPLICATION_JSON)
        public Response uploadZipToTransaction(@PathParam("transactionId") String transactionId,
            InputStream inputStreamObject) {
            return expectedResponse.post();
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
        public Response uploadObjectGroup(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
            @PathParam("version") Integer version, ObjectDto objectDto) {
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
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response upload(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
            @PathParam("version") Integer version, InputStream uploadedInputStream) {
            return expectedResponse.get();
        }

        @Path("/units/{unitId}/objects/{usage}/{version}/binary")
        @GET
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response download(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
            @PathParam("version") Integer version) {
            return expectedResponse.get();
        }
    }
}