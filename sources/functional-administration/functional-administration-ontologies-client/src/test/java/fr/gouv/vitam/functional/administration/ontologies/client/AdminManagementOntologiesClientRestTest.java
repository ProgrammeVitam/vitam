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
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminManagementOntologiesClientRestTest extends ResteasyTestApplication {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    protected static AdminManagementOntologiesClientRest client;
    private static final Integer TENANT_ID = 1;

    protected static ExpectedResults mock;

    static AdminManagementOntologiesClientFactory factory = AdminManagementOntologiesClientFactory.getInstance();

    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(AdminManagementOntologiesClientRestTest.class, factory);


    @BeforeClass
    public static void init() {
        client = (AdminManagementOntologiesClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        mock = mock(ExpectedResults.class);

        return Sets.newHashSet(new MockResource(mock));
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
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllOntologiesThenReturnEmpty()
        throws InvalidParseOperationException, VitamClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get())
            .thenReturn(Response.status(Response.Status.OK).entity(new RequestResponseOK<OntologyModel>()).build());
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
    public void findAllOntologiesThenReturnTwo()
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
