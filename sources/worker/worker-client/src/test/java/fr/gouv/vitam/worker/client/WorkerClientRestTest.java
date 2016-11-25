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
import static org.mockito.Mockito.when;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;

public class WorkerClientRestTest extends VitamJerseyTest {
    protected static final String HOSTNAME = "localhost";
    private static final String DUMMY_REQUEST_ID = "reqId";
    protected static int serverPort;
    protected WorkerClientRest client;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public WorkerClientRestTest() {
        super(WorkerClientFactory.getInstance());
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (WorkerClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
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
    }
    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

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

    @RunWithCustomExecutor
    @Test
    public void submitOK() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);

        final ItemStatus result = new ItemStatus("StepId");

        final ItemStatus itemStatus1 = new ItemStatus("checkSeda");
        itemStatus1.setMessage("CHECK_MANIFEST_OK");
        final StatusCode status = StatusCode.OK;
        itemStatus1.increment(status);
        result.setItemsStatus("checkSeda", itemStatus1);

        final ItemStatus itemStatus2 = new ItemStatus("CheckVersion");
        itemStatus2.setMessage("CHECK_VERSION_OK");
        itemStatus2.increment(status);
        result.setItemsStatus("CheckVersion", itemStatus2);

        when(mock.post()).thenReturn(Response.status(Response.Status.OK).entity(result).build());
        final ItemStatus responses =
            client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
        assertNotNull(responses);
        assertEquals(StatusCode.OK, responses.getItemsStatus().get("checkSeda").getGlobalStatus());
        assertEquals(StatusCode.OK, responses.getItemsStatus().get("CheckVersion").getGlobalStatus());
        assertEquals(StatusCode.OK, responses.getGlobalStatus());
    }

    @RunWithCustomExecutor
    @Test(expected = WorkerNotFoundClientException.class)
    public void submitNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
    }

    @RunWithCustomExecutor
    @Test(expected = WorkerServerClientException.class)
    public void submitException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.submitStep(new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters()));
    }

}
