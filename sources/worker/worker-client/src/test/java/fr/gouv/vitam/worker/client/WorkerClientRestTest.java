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
package fr.gouv.vitam.worker.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;

public class WorkerClientRestTest extends JerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static int serverPort;
    protected final WorkerClientRest client;
    private static JunitHelper junitHelper;

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response get();

        Response post();

        Response head();

        Response delete();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        junitHelper.releasePort(serverPort);
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, Integer.toString(serverPort));
        mock = mock(ExpectedResults.class);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        return resourceConfig.registerInstances(new MockResource(mock));
    }

    @Path("/worker/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;
        public static final String APPLICATION_ZIP = "application/zip";

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("/tasks")
        public Response submitStep() {
            return expectedResponse.post();
        }
    }

    public WorkerClientRestTest() {
        client = new WorkerClientRest(new ClientConfigurationImpl(HOSTNAME, serverPort), "/worker/v1", false);
    }

    @Test
    public void submitOK() throws Exception {
        final String jsonResult =
            "[{\"processId\":null,\"status\":\"OK\",\"outcomeMessages\":{\"checkSeda\":\"CHECK_MANIFEST_OK\"},\"stepResponses\":{},\"messageIdentifier\":\"vitam\",\"errorNumber\":0,\"value\":\"OK\"}," +
                "{\"processId\":null,\"status\":\"OK\",\"outcomeMessages\":{\"CheckVersion\":\"CHECK_VERSION_OK\"},\"stepResponses\":{},\"messageIdentifier\":\"\",\"errorNumber\":0,\"value\":\"OK\"}," +
                "{\"processId\":null,\"status\":\"OK\",\"outcomeMessages\":{\"CheckObjectsNumber\":\"CHECK_OBJECT_NUMBER_OK\"},\"stepResponses\":{},\"messageIdentifier\":\"\",\"errorNumber\":0,\"value\":\"OK\"}," +
                "{\"processId\":null,\"status\":\"OK\",\"outcomeMessages\":{\"ExtractSeda\":\"EXTRACT_MANIFEST_OK\"},\"stepResponses\":{},\"messageIdentifier\":\"\",\"errorNumber\":0,\"value\":\"OK\"}," +
                "{\"processId\":null,\"status\":\"OK\",\"outcomeMessages\":{\"CheckConformity\":\"CHECK_CONFORMITY_OK\"},\"stepResponses\":{},\"messageIdentifier\":\"\",\"errorNumber\":0,\"value\":\"OK\"}]";

        when(mock.post()).thenReturn(Response.status(Response.Status.OK).entity(jsonResult).build());
        final List<EngineResponse> responses =
            client.submitStep("requestId",
                new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
        assertNotNull(responses);
        assertEquals(StatusCode.OK, responses.get(0).getStatus());
        assertEquals(StatusCode.OK, responses.get(1).getStatus());
        assertEquals("OK", responses.get(2).getValue());
    }

    @Test(expected = WorkerNotFoundClientException.class)
    public void submitNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.submitStep("requestId", new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
    }

    @Test(expected = WorkerServerClientException.class)
    public void submitException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.submitStep("requestId", new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
    }

}
