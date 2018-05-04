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
package fr.gouv.vitam.functional.administration.ontologies.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AdminManagementOntologiesClientRestTest extends VitamJerseyTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    protected AdminManagementOntologiesClientRest client;
    private static final Integer TENANT_ID = 1;
    
    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public AdminManagementOntologiesClientRestTest() {
        super(AdminManagementOntologiesClientFactory.getInstance());
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (AdminManagementOntologiesClientRest) getClient();
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

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }


    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }


    @Path("/adminmanagement/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/ontologies/cache")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getCachedOntologies() {
            return expectedResponse.get();
        }

    }

    /**
     * Test that profiles is reachable and does not return elements
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllOntologiesThenReturnEmpty()
            throws FileNotFoundException, InvalidParseOperationException, VitamClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get()).thenReturn(Response.status(Response.Status.OK).entity(new RequestResponseOK<OntologyModel>()).build());
        RequestResponse resp = client.findOntologiesForCache(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }
    
    /**
     * Test that ontologies is reachable and return two elements as expected
     *
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllAgenciesThenReturnTwo()
            throws FileNotFoundException, InvalidParseOperationException, VitamClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Response.Status.OK)
                .entity(new RequestResponseOK<OntologyModel>().addAllResults(getOntologies())).build());
        RequestResponse resp = client.findOntologiesForCache(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
        assertThat(((RequestResponseOK) resp).getResults().iterator().next()).isInstanceOf(OntologyModel.class);
    }
    
    private List<OntologyModel> getOntologies() throws FileNotFoundException, InvalidParseOperationException {
        File fileOntologies = PropertiesUtils.getResourceFile("ontologies_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileOntologies, new TypeReference<List<OntologyModel>>() {
        });
    }

}
