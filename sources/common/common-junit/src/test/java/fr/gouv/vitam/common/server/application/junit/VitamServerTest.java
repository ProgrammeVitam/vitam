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
package fr.gouv.vitam.common.server.application.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.junit.Test;

import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.server.VitamServerInterface;
import fr.gouv.vitam.common.server.application.VitamApplicationInterface;
import fr.gouv.vitam.common.server.application.configuration.VitamApplicationConfigurationInterface;

/**
 * VitamJerseyTest and MinimalTestVitamApplicationFactory Tests
 */
public class VitamServerTest {

    private static class ServerTest implements VitamServerInterface {

        @Override
        public int getPort() {
            return 10;
        }

    }
    private static class ConfigurationTest implements VitamApplicationConfigurationInterface {

        @Override
        public String getJettyConfig() {
            return "jetty-config.xml";
        }

        @Override
        public VitamApplicationConfigurationInterface setJettyConfig(String jettyConfig) {
            return this;
        }

        @Override
        public List<Integer> getTenants() {
            return new ArrayList<Integer>();
        }

        @Override
        public VitamApplicationConfigurationInterface setTenants(List<Integer> tenants) {
            return this;
        }

    }
    private static abstract class ApplicationAbstractTest<A extends VitamApplicationInterface<A, C>, C extends VitamApplicationConfigurationInterface>
        implements VitamApplicationInterface<A, C> {

        @Override
        public VitamServerInterface getVitamServer() {
            return new ServerTest();
        }

        @Override
        public void start() throws VitamApplicationServerException {
            // Nothing to do
        }

        @Override
        public void stop() throws VitamApplicationServerException {
            // Nothing to do
        }

    }
    private static class ApplicationTest extends ApplicationAbstractTest<ApplicationTest, ConfigurationTest> {

    }

    @Test
    public void testVitamJerseyTest() throws VitamApplicationServerException {
        final ApplicationTest application = new ApplicationTest();
        final VitamJerseyTest<ApplicationTest> vitamJerseyTest =
            new VitamJerseyTest<ApplicationTest>(new VitamClientFactoryInterface<MockOrRestClient>() {

                @Override
                public Client getHttpClient() {
                    return null;
                }

                @Override
                public Client getHttpClient(boolean useChunkedMode) {
                    return null;
                }

                @Override
                public MockOrRestClient getClient() {
                    return null;
                }

                @Override
                public String getResourcePath() {
                    return null;
                }

                @Override
                public String getServiceUrl() {
                    return null;
                }

                @Override
                public ClientConfig getDefaultConfigCient() {
                    return null;
                }

                @Override
                public ClientConfig getDefaultConfigCient(boolean chunkedMode) {
                    return null;
                }

                @Override
                public ClientConfiguration getClientConfiguration() {
                    return null;
                }

                @Override
                public fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType getVitamClientType() {
                    return null;
                }

                @Override
                public VitamClientFactoryInterface<?> setVitamClientType(
                    fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType vitamClientType) {
                    return null;
                }

                @Override
                public void changeServerPort(int port) {

            }
            }) {

                @Override
                public StartApplicationResponse<ApplicationTest> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    return new StartApplicationResponse<ApplicationTest>()
                        .setApplication(application).setServerPort(10);
                }

                @Override
                public void setup() {
                    // Nothing to do
                }

            };
        assertEquals(10, vitamJerseyTest.getServerPort());
        assertEquals(application, vitamJerseyTest.getApplication());
        assertNull(vitamJerseyTest._client);
        try {
            vitamJerseyTest.startTest();
            vitamJerseyTest.endTest();
        } catch (final VitamApplicationServerException e) {
            fail("Should not raized an exception");
        }

        final MinimalTestVitamApplicationFactory<ApplicationTest> minimalTestVitamApplicationFactory =
            new MinimalTestVitamApplicationFactory<ApplicationTest>() {

                @Override
                public StartApplicationResponse<ApplicationTest> startVitamApplication(
                    int reservedPort) throws IllegalStateException {
                    return new StartApplicationResponse<ApplicationTest>()
                        .setApplication(application).setServerPort(10);
                }
            };
        minimalTestVitamApplicationFactory.findAvailablePortSetToApplication();
        final StartApplicationResponse<ApplicationTest> startApplicationResponde =
            minimalTestVitamApplicationFactory.startAndReturn(application);
        assertEquals(10, startApplicationResponde.getServerPort());
    }

    @Test
    public void testResponseHelper() {
        Response response = ResponseHelper.getOutboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM, null);
        assertTrue(response.getStatus() == Status.OK.getStatusCode());
        assertTrue(response.readEntity(InputStream.class) instanceof InputStream);

        response = ResponseHelper.getOutboundResponse(Status.OK, null, MediaType.APPLICATION_OCTET_STREAM, null);
        assertTrue(response.getStatus() == Status.OK.getStatusCode());
        assertTrue(response.readEntity(String.class).equals(""));

        final Map<String, String> map = new HashMap<>();
        map.put("testkey", "testvalue");
        response = ResponseHelper.getOutboundResponse(Status.OK, null, MediaType.APPLICATION_OCTET_STREAM, map);
        assertTrue(response.getStatus() == Status.OK.getStatusCode());
        assertTrue(response.readEntity(String.class).equals(""));
        assertTrue(response.getHeaderString("testkey").equals("testvalue"));

        try {
            response = ResponseHelper.getOutboundResponse(null, null, MediaType.APPLICATION_OCTET_STREAM, map);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        response = ResponseHelper.getOutboundResponse(Status.OK, null, null, null);
        assertTrue(response.getHeaderString("Content-Type").equals(MediaType.APPLICATION_JSON));
    }
}
