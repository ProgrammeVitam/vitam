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
package fr.gouv.vitam.processing.management.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnauthorizeException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;

public class WorkflowProcessingManagementClientTest extends VitamJerseyTest {
    private static ProcessingManagementClient client;
    Supplier<Response> mock;
    private static final String WORKFLOWID = "json1";
    private static final String CONTAINER = "c1";

    public WorkflowProcessingManagementClientTest() {
        super(ProcessingManagementClientFactory.getInstance());
    }

    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (ProcessingManagementClient) getClient();
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
            mock = mock(Supplier.class);
            resourceConfig.registerInstances(new ProcessingResource(mock));
        }
    }
    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
    }

    @Path("/processing/v1")
    public static class ProcessingResource {
        private final Supplier<Response> expectedResponse;

        public ProcessingResource(Supplier<Response> expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("operations")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response executeVitamProcess(ProcessingEntry workflow) {
            return expectedResponse.get();
        }
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenNotFoundWorkflowWhenProcessingThenReturnNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenIllegalArgumementWhenProcessingThenReturnIllegalPrecondtionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }

    @Test(expected = ProcessingUnauthorizeException.class)
    public void givenUnauthorizedOperationWhenProcessingThenReturnUnauthorized() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }

    @Test
    public void givenBadRequestWhenProcessingThenReturnBadRequest() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).entity(desired).build());
        final ItemStatus ret = client.executeVitamProcess(CONTAINER, WORKFLOWID);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }

    @Test(expected = ProcessingInternalServerException.class)
    public void givenInternalServerErrorWhenProcessingThenReturnInternalServerError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.executeVitamProcess(CONTAINER, WORKFLOWID);
    }

    @Test
    public void executeVitamProcessOk() throws Exception {
        final ItemStatus desired = new ItemStatus("ID");
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(desired).build());
        final ItemStatus ret = client.executeVitamProcess(CONTAINER, WORKFLOWID);
        assertNotNull(ret);
        assertEquals(desired.getGlobalStatus(), ret.getGlobalStatus());
    }
}
