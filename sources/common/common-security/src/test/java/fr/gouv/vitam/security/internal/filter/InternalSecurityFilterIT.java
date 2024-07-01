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
package fr.gouv.vitam.security.internal.filter;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SSLKey;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.SslConfig;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InternalSecurityFilterIT extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InternalSecurityFilterIT.class);

    // Static
    private static volatile InternalSecurityFilter currentInternalSecurityFilter;

    private GenericContainer<?> reverseContainer;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    public VitamServerTestRunner vitamServerTestRunner;

    public void initializeTestServer(boolean allowSslClientHeader) throws Exception {
        LOGGER.info("Starting app server...");
        SslConfig sslConfig = new SslConfig(
            PropertiesUtils.getResourceFile("tls/app/app.jks").getAbsolutePath(),
            "azerty",
            PropertiesUtils.getResourceFile("tls/app/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        // Hack: Using a static variable to pass InternalSecurityFilter instance to the Application
        InternalSecurityFilterIT.currentInternalSecurityFilter = new InternalSecurityFilter(allowSslClientHeader);
        vitamServerTestRunner = new VitamServerTestRunner(
            InternalSecurityFilterIT.class,
            InternalSecurityFilterIT.class,
            sslConfig,
            null,
            false,
            false,
            false,
            false,
            false
        );
        vitamServerTestRunner.start();
        LOGGER.info("Running test with app port " + vitamServerTestRunner.getBusinessPort());

        Testcontainers.exposeHostPorts(vitamServerTestRunner.getBusinessPort());
        LOGGER.info("Port " + vitamServerTestRunner.getBusinessPort() + " exposed on host to containers");
    }

    @After
    public void after() throws Exception {
        if (vitamServerTestRunner != null) {
            vitamServerTestRunner.runAfter();
        }
        if (reverseContainer != null) {
            reverseContainer.stop();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessWithoutReverseProxyWithClientCert() throws Exception {
        initializeTestServer(false);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientKeyStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/client.p12").getAbsolutePath(),
            "azerty"
        );
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration(List.of(clientKeyStore), List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                vitamServerTestRunner.getBusinessPort(),
                true,
                sslConfiguration,
                false
            )
        );
        String hello;
        try (TestClient client = factory.getClient()) {
            hello = client.sayHello();
        }

        assertThat(hello).isEqualTo("hi!");
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessWithoutReverseProxyWithoutClientCert() throws Exception {
        initializeTestServer(false);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.setTruststore(List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                vitamServerTestRunner.getBusinessPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughNginxReverseProxyAndHeaderCertDisabledWithClientCert() throws Exception {
        initializeTestServer(false);
        initializeNginxContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientKeyStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/client.p12").getAbsolutePath(),
            "azerty"
        );
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration(List.of(clientKeyStore), List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughNginxReverseProxyAndHeaderCertEnabledWithClientCert() throws Exception {
        initializeTestServer(true);
        initializeNginxContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientKeyStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/client.p12").getAbsolutePath(),
            "azerty"
        );
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration(List.of(clientKeyStore), List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );
        String hello;
        try (TestClient client = factory.getClient()) {
            hello = client.sayHello();
        }

        assertThat(hello).isEqualTo("hi!");
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughNginxReverseProxyAndHeaderCertDisabledWithoutClientCert() throws Exception {
        initializeTestServer(false);
        initializeNginxContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.setTruststore(List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughNginxReverseProxyAndHeaderCertEnabledWithoutClientCert() throws Exception {
        initializeTestServer(true);
        initializeNginxContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.setTruststore(List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughHttpdReverseProxyAndHeaderCertEnabledWithClientCert() throws Exception {
        initializeTestServer(true);
        initializeHttpdContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientKeyStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/client.p12").getAbsolutePath(),
            "azerty"
        );
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration(List.of(clientKeyStore), List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        String hello;
        try (TestClient client = factory.getClient()) {
            hello = client.sayHello();
        }

        assertThat(hello).isEqualTo("hi!");
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughHttpdReverseProxyAndHeaderCertDisabledWithClientCert() throws Exception {
        initializeTestServer(false);
        initializeHttpdContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientKeyStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/client.p12").getAbsolutePath(),
            "azerty"
        );
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration(List.of(clientKeyStore), List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughHttpdReverseProxyAndHeaderCertEnabledWithoutClientCert() throws Exception {
        initializeTestServer(true);
        initializeHttpdContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.setTruststore(List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessThroughHttpdReverseProxyAndHeaderCertDisabledWithoutClientCert() throws Exception {
        initializeTestServer(false);
        initializeHttpdContainer();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        SSLKey clientTrustStore = new SSLKey(
            PropertiesUtils.getResourceFile("tls/client/truststore.jks").getAbsolutePath(),
            "azerty"
        );
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.setTruststore(List.of(clientTrustStore));
        MyVitamClientFactory factory = new MyVitamClientFactory(
            new SecureClientConfigurationImpl(
                "localhost",
                reverseContainer.getFirstMappedPort(),
                true,
                sslConfiguration,
                false
            )
        );

        try (TestClient client = factory.getClient()) {
            assertThatThrownBy(client::sayHello)
                .isInstanceOf(VitamClientInternalException.class)
                .hasMessageContaining("Request do not contain any X509Certificate");
        }
    }

    private void initializeNginxContainer() throws Exception {
        updateReverseConfigWithPort("tls/reverse-nginx/nginx.conf.template");

        String nginxContainerVersion = System.getProperty("nginxContainerVersion");

        reverseContainer = new GenericContainer<>(DockerImageName.parse(nginxContainerVersion))
            .withClasspathResourceMapping("tls/reverse-nginx/", "/etc/nginx/", BindMode.READ_ONLY)
            .withCommand("nginx-debug", "-g", "daemon off;")
            .withAccessToHost(true)
            .withExposedPorts(443);

        reverseContainer.start();

        attachContainerLogs();

        LOGGER.info("Running test with nginx port " + reverseContainer.getFirstMappedPort());
    }

    private void initializeHttpdContainer() throws Exception {
        updateReverseConfigWithPort("tls/reverse-httpd/httpd.conf.template");

        String httpdContainerVersion = System.getProperty("httpdContainerVersion");

        reverseContainer = new GenericContainer<>(DockerImageName.parse(httpdContainerVersion))
            .withClasspathResourceMapping("tls/reverse-httpd/", "/usr/local/apache2/conf/", BindMode.READ_ONLY)
            .withAccessToHost(true)
            .withExposedPorts(443);

        reverseContainer.start();

        attachContainerLogs();

        LOGGER.info("Running test with httpd port " + reverseContainer.getFirstMappedPort());
    }

    private void updateReverseConfigWithPort(String templateResourcesFile) throws IOException {
        File templateConfFile = PropertiesUtils.getResourceFile(templateResourcesFile);
        File confFile = new File(
            templateConfFile.getParentFile(),
            StringUtils.remove(templateConfFile.getName(), ".template")
        );
        String config = FileUtils.readFileToString(templateConfFile, StandardCharsets.UTF_8);
        String updatedConfig = config.replaceAll("####PORT####", vitamServerTestRunner.getBusinessPort() + "");
        FileUtils.write(confFile, updatedConfig, StandardCharsets.UTF_8);
        LOGGER.info("Running test with conf file " + confFile);
    }

    private void attachContainerLogs() {
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass()));
        reverseContainer.followOutput(logConsumer);
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new EchoResource(), currentInternalSecurityFilter, new SanityCheckerCommonFilter());
    }

    @Path("/")
    public static class EchoResource {

        @GET
        @Path("hello")
        @Produces(MediaType.TEXT_PLAIN)
        public Response sayHello() {
            return Response.ok("hi!").build();
        }
    }

    private static class MyVitamClientFactory extends VitamClientFactory<TestClient> {

        public MyVitamClientFactory(ClientConfigurationImpl clientConfiguration) {
            super(clientConfiguration, "/");
            super.enableUseAuthorizationFilter();
        }

        @Override
        public TestClient getClient() {
            return new TestClient(this);
        }

        @Override
        public void changeResourcePath(String resourcePath) {
            super.changeResourcePath(resourcePath);
        }

        @Override
        public void changeServerPort(int port) {
            super.changeServerPort(port);
        }
    }

    public static class TestClient extends DefaultClient {

        public TestClient(VitamClientFactoryInterface<TestClient> factory) {
            super(factory);
        }

        public String sayHello() throws VitamClientInternalException {
            VitamRequestBuilder request = VitamRequestBuilder.get()
                .withHeader("Host", "app")
                .withPath("hello")
                .withAccept(MediaType.TEXT_PLAIN_TYPE);

            try (Response response = make(request)) {
                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    throw new VitamClientInternalException(
                        "Expected 200, got " + response.getStatus() + "\n" + response.readEntity(String.class)
                    );
                }
                return response.readEntity(String.class);
            }
        }
    }
}
