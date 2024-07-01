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
package fr.gouv.vitam.access.external.client.v2;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessExternalClientV2RestTest extends ResteasyTestApplication {

    protected static AccessExternalClientV2Rest client;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    final String queryDsql = "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], \"$projection\" : {} }";
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    private static final ExpectedResults mock = mock(ExpectedResults.class);
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        AccessExternalClientV2RestTest.class,
        AccessExternalClientV2Factory.getInstance()
    );

    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        client = (AccessExternalClientV2Rest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/access-external/v2")
    public static class MockResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("/dipexport")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response exportDIP(DipRequest dipRequest) {
            return expectedResponse.post();
        }
    }

    /***
     *
     * DIP export
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void exportDIP() throws Exception {
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build()
        );
        DipRequest exportRequest = new DipRequest(JsonHandler.getFromString(queryDsql));
        assertThat(
            client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), exportRequest)
        ).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNotFoundThenNotFound() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        DipRequest exportRequest = new DipRequest(JsonHandler.getFromString(queryDsql));
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(() -> client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), exportRequest))
            .withMessage("Error with the response, get status: '404' and reason 'Not Found'.");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNoQueryThen415() {
        when(mock.post()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), (DipRequest) null)
            )
            .withMessage("DipRequest cannot be null.");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPBadQueryThenPreconditionFailed() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        DipRequest exportRequest = new DipRequest(JsonHandler.getFromString(queryDsql));
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(() -> client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), exportRequest))
            .withMessage("Error with the response, get status: '412' and reason 'Precondition Failed'.");
    }
}
