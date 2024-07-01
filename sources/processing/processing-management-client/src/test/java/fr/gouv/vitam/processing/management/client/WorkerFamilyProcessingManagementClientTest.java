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
package fr.gouv.vitam.processing.management.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class WorkerFamilyProcessingManagementClientTest extends ResteasyTestApplication {

    private static ProcessingManagementClient client;

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    static ProcessingManagementClientFactory factory = ProcessingManagementClientFactory.getInstance();
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        WorkerFamilyProcessingManagementClientTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (ProcessingManagementClientRest) vitamServerTestRunner.getClient();
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

    @Path("/processing/v1/worker_family")
    public static class MockResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("/{id_family}/workers/{id_worker}")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response registerWorker(
            @Context HttpHeaders headers,
            @PathParam("id_family") String idFamily,
            @PathParam("id_worker") String idWorker,
            String workerInformation
        ) {
            return expectedResponse.post();
        }

        @Path("/{id_family}/workers/{id_worker}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response unregisterWorker(
            @Context HttpHeaders headers,
            @PathParam("id_family") String idFamily,
            @PathParam("id_worker") String idWorker
        ) {
            return expectedResponse.delete();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenBadParametersThenThrowsException() throws Exception {
        client.registerWorker("familyId", "workerId", null);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void givenBadWorkerDescriptionThenThrowsBadRequest() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.BAD_REQUEST).build());
        client.registerWorker("familyId", "workerId", getDefaultWorkerBean());
    }

    @Test
    public void givenCorrectWorkerFamilyUnregisteringThenOk() throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"success\" :\"Worker idWorker created \"}").build()
        );
        client.registerWorker("familyId", "workerId", getDefaultWorkerBean());
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void givenUnknownFamilyUnregisteringThenThrowsNotFound() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.unregisterWorker("familyId", "workerId");
    }

    @Test
    public void givenCorrectWorkerUnregisteringThenOk() throws Exception {
        when(mock.delete()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"success\" :\"Worker idWorker deleted \"}").build()
        );
        client.unregisterWorker("familyId", "workerId");
    }

    private WorkerBean getDefaultWorkerBean() throws Exception {
        final String json_worker =
            "{ \"name\" : \"workername\", \"family\" : \"familyname\", \"capacity\" : 10, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"89102\" } }";
        return JsonHandler.getFromString(json_worker, WorkerBean.class);
    }
}
