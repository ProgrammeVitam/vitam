/**
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
 */
/**
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
 */
package fr.gouv.vitam.common.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.StatusMessage;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;

public class AbstractClientTest extends JerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final String RESOURCE_PATH = "/vitam-test/v1";
    protected static int serverPort;
    protected final BasicClient client;
    private static JunitHelper junitHelper;

    protected AbstractClientTest.ExpectedResults mock;

    interface ExpectedResults {
        Response get();
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
        mock = mock(AbstractClientTest.ExpectedResults.class);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        return resourceConfig.registerInstances(new MockResource(mock));
    }

    @Path(RESOURCE_PATH)
    public static class MockResource {
        private final AbstractClientTest.ExpectedResults expectedResponse;

        public MockResource(AbstractClientTest.ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }        
    }

    public AbstractClientTest() throws VitamClientException {
        client = new FakeClient();
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.getStatus();
    }

    @Test
    public void constructorWithGivenClient() throws VitamClientException {
        Client mock = mock(Client.class);
        FakeClient fakeClient = new FakeClient(mock);
        assertEquals(mock, fakeClient.getClient());
        assertEquals("http://" + HOSTNAME + ":" + serverPort + client.getResourcePath(), fakeClient.getServiceUrl());
    }

    @Test
    public void statusExecutionWithBody() throws Exception {
        when(mock.get()).thenReturn(
            Response.status(Response.Status.OK).entity("{\"pid\":\"1\",\"name\":\"name1\", \"role\":\"role1\"}")
                .build());
        final StatusMessage message = client.getStatus();
        assertEquals("name1", message.getName());
        assertEquals("role1", message.getRole());
        assertEquals(1, message.getPid());
    }

    
    @Test(expected = VitamClientException.class)
    public void failsStatusExecution() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_IMPLEMENTED).build());
        client.getStatus();
    }

    @Test
    public void testShutdown() throws Exception {
        client.shutdown();
    }

    private class FakeClient extends AbstractClient {
        FakeClient() throws VitamClientException {
            super(new ClientConfigurationImpl(HOSTNAME, serverPort), RESOURCE_PATH, false);
        }

        FakeClient(Client client) throws VitamClientException {
            super(new ClientConfigurationImpl(HOSTNAME, serverPort), RESOURCE_PATH, client);
        }

        public <R> R handleCommonResponseStatus(Response response, Class<R> responseType) throws VitamClientException {
            return null;
        }
    }
}
