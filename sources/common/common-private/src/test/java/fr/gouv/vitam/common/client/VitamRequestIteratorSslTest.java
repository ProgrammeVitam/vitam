/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SSLKey;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerDisconnectException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.SslConfig;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class VitamRequestIteratorSslTest extends ResteasyTestApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIteratorSslTest.class);


    private static final String BASE_URI = "/ingest-ext/v1";
    private static final String INGEST_EXTERNAL_CLIENT_CONF = "standard-client-secure.conf";
    private static final String INGEST_EXTERNAL_SERVER_CONF = "standard-server-secure.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_TLS_V1 = "standard-client-secure_tlsv1.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_TLS_V11 = "standard-client-secure_tlsv11.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_TLS_V12 = "standard-client-secure_tlsv12.conf";

    private static boolean startup = true;
    private final static ExpectedResults mock = mock(ExpectedResults.class);
    private static VitamClientFactory<DefaultClient> factory;
    private static VitamClientFactory<DefaultClient> customFactory;


    private final static SecureClientConfiguration configurationServer =
        changeConfigurationFile(INGEST_EXTERNAL_SERVER_CONF);

    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(VitamRequestIteratorSslTest.class, VitamServerTestRunner.AdminApp.class,
            new SslConfig(
                configurationServer.getSslConfiguration().getKeystore().iterator().next().getKeyPath(),
                configurationServer.getSslConfiguration().getKeystore().iterator().next().getKeyPassword(),
                configurationServer.getSslConfiguration().getTruststore().iterator().next().getKeyPath(),
                configurationServer.getSslConfiguration().getTruststore().iterator().next().getKeyPassword()
                ),
            null,
            false, false, false, true, false);



    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new SslResource(mock));
    }

    @Path(BASE_URI)
    @javax.ws.rs.ApplicationPath("webresources")
    public static class SslResource extends ApplicationStatusResource {
        private final ExpectedResults mock;

        public SslResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @GET
        @Path("/iterator")
        @Produces(MediaType.APPLICATION_JSON)
        public Response iterator(@Context HttpHeaders headers) {
            final Response response = mock.get();
            startup = false;
            return response;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF);
        configuration.setServerPort(vitamServerTestRunner.getBusinessPort());

        factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        factory.disableUseAuthorizationFilter();
        LOGGER.warn("Start ClientFactory: " + factory);
    }

    public static void initClientFactoryWithSpecificProtocolVersion(String configFile) {
        final SecureClientConfiguration configuration = changeConfigurationFile(configFile);
        configuration.setServerPort(vitamServerTestRunner.getBusinessPort());

        customFactory =
                new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                    @Override
                    public DefaultClient getClient() {
                        return new DefaultClient(this);
                    }

                };
        customFactory.disableUseAuthorizationFilter();
        LOGGER.warn("Start ClientFactory: " + factory);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
        try {
            factory.shutdown();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

    }

    @Test
    public void testClientBuilder() throws Exception {
        final SSLKey key = new SSLKey("tls/client/client.p12", "azerty4");
        final ArrayList<SSLKey> truststore = new ArrayList<>();
        truststore.add(key);
        final SSLConfiguration sslConfig = new SSLConfiguration(truststore, truststore);
        final SecureClientConfiguration configuration =
            new SecureClientConfigurationImpl("host", 8443, true, sslConfig, false);
        final VitamClientFactory<DefaultClient> factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        try (DefaultClient client = factory.getClient()) {
            // Only Apache Pool has this
            assertNull(client.getClient().getHostnameVerifier());
        } finally {
            try {
                factory.shutdown();
            } catch (Exception e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    static final SecureClientConfiguration changeConfigurationFile(String configurationPath) {
        SecureClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                SecureClientConfigurationImpl.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Configuration cannot be read: " + configurationPath, e);
        }
        if (configuration == null) {
            throw new IllegalStateException("Configuration cannot be read: " + configurationPath);
        }
        return configuration;
    }

    @Test
    public void givenCertifValidThenReturnOK() {
        LOGGER.warn("Start Client configuration: " + factory);
        try (final DefaultClient client = factory.getClient()) {
            client.checkStatus();
        } catch (final VitamException e) {
            LOGGER.error("THIS SHOULD NOT RAIZED AN EXCEPTION", e);
            fail("THIS SHOULD NOT RAIZED AN EXCEPTION");
        }
    }

    @Test
    public void givenTlsProtocolVersion12ThenShouldNotThrownException() {
        initClientFactoryWithSpecificProtocolVersion(INGEST_EXTERNAL_CLIENT_CONF_TLS_V12);
        LOGGER.warn("Start Client configuration: " + customFactory);
        try (final DefaultClient client = customFactory.getClient()) {
            client.checkStatus();
        } catch (final VitamException e) {
            LOGGER.error("THIS SHOULD NOT RAIZED AN EXCEPTION", e);
            fail("THIS SHOULD NOT RAIZED AN EXCEPTION");
        }
    }

    @Test
    public void givenTlsProtocolVersion1ThenThrownException() {
        initClientFactoryWithSpecificProtocolVersion(INGEST_EXTERNAL_CLIENT_CONF_TLS_V1);
        LOGGER.warn("Start Client configuration: " + customFactory);
        try (final DefaultClient client = customFactory.getClient()) {
            client.checkStatus();
            fail("THIS SHOULD RAIZED AN EXCEPTION");
        } catch (final VitamException e) {
            assertTrue(e instanceof VitamApplicationServerDisconnectException);
            assertTrue(e.getMessage().contains("Client is disconnected"));
        }
    }

    @Test
    public void givenTlsProtocolVersion11ThenThrownException() {
        initClientFactoryWithSpecificProtocolVersion(INGEST_EXTERNAL_CLIENT_CONF_TLS_V11);
        LOGGER.warn("Start Client configuration: " + customFactory);
        try (final DefaultClient client = customFactory.getClient()) {
            client.checkStatus();
            fail("THIS SHOULD RAIZED AN EXCEPTION");
        } catch (final VitamException e) {
            assertTrue(e instanceof VitamApplicationServerDisconnectException);
            assertThat(e.getMessage()).isEqualTo("Client is disconnected");
        }
    }

    @Test
    public void testIterator() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            builder = Response.status(Status.OK);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final JsonNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            // Ensure next hasNext should be False without exception
            when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST.getStatusCode()).build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorEmpty() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.NOT_FOUND);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorFailed() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            try {
                assertFalse(iterator.hasNext());
                fail("should raized an exception");
            } catch (final VitamRuntimeException e) {

            }
        }
    }

    @Test
    public void testIteratorShortList() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.OK);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorThirdShort() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            builder = Response.status(Status.NOT_FOUND);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorStopBefore() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            iterator.close();
            assertFalse(iterator.hasNext());
        }
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
                new VitamRequestIterator<>(client, VitamRequestBuilder.get().withPath("/iterator"), ObjectNode.class)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            iterator.close();
            assertFalse(iterator.hasNext());
        }
    }

}
