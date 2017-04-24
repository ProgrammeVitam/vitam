/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;

import fr.gouv.vitam.common.auth.web.filter.X509AuthenticationFilter;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.benchmark.BenchmarkConfiguration;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

public class DefaultSslHeaderClientTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultSslHeaderClientTest.class);

    private static final String BASE_URI = "/ingest-ext/v1";
    private static final String INGEST_EXTERNAL_CONF = "standard-application-ssl-header-test.conf";
    private static final String SHIRO_FILE = "shiro.ini";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_NOKEY = "standard-client-secure_nokey.conf";
    private static TestVitamApplication application;
    private static int serverPort;


    private static String pem = null;
    private static String pemExpired = null;
    private static String pemNotGranted = null;


    @Path(BASE_URI)
    @javax.ws.rs.ApplicationPath("webresources")
    private static class SslResource extends ApplicationStatusResource {
        // Empty
    }

    private static class TestVitamApplication
        extends AbstractVitamApplication<TestVitamApplication, BenchmarkConfiguration> {

        protected TestVitamApplication(String config) {
            super(BenchmarkConfiguration.class, config);
        }

        protected TestVitamApplication(BenchmarkConfiguration config) {
            super(BenchmarkConfiguration.class, config);
        }

        @Override
        protected void platformSecretConfiguration() {
            // Nothing
        }

        @Override
        protected void checkJerseyMetrics(ResourceConfig resourceConfig) {
            // Nothing
        }

        @Override
        protected void setFilter(ServletContextHandler context) throws VitamApplicationServerException {
            File shiroFile = null;
            try {
                shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
            } catch (final FileNotFoundException e) {
                throw new VitamApplicationServerException(e.getMessage());
            }
            LOGGER.info("Start Shiro configuration");
            context.setInitParameter("shiroConfigLocations", "file:" + shiroFile.getAbsolutePath());
            context.addEventListener(new EnvironmentLoaderListener());
            context.addFilter(ShiroFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.register(new SslResource());
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }

    private static String x509CertificateToPem(X509Certificate cert) throws CertificateEncodingException {
        BASE64Encoder encoder = new BASE64Encoder();

        StringWriter sw = new StringWriter();
        sw.write(X509Factory.BEGIN_CERT);
        sw.write("\n");
        sw.write(encoder.encode(cert.getEncoded()));
        sw.write("\n");
        sw.write(X509Factory.END_CERT);
        return Base64.getEncoder().encodeToString(sw.toString().getBytes());
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        KeyStore p12 = KeyStore.getInstance("pkcs12");
        p12.load(PropertiesUtils.getConfigAsStream("tls/client/client.p12"), "vitam2016".toCharArray());
        Enumeration e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            pem = x509CertificateToPem(c);
            break;
            /*Principal subject = c.getSubjectDN();
            String subjectArray[] = subject.toString().split(",");
            for (String s : subjectArray) {
                String[] str = s.trim().split("=");
                String key = str[0];
                String value = str[1];
                System.out.println(key + " - " + value);
            }*/
        }

        p12.load(PropertiesUtils.getConfigAsStream("tls/client/client_expired.p12"), "vitam2016".toCharArray());
        e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            pemExpired = x509CertificateToPem(c);
            break;
        }

        p12.load(PropertiesUtils.getConfigAsStream("tls/client/client_notgranted.p12"), "vitam2016".toCharArray());
        e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            pemNotGranted = x509CertificateToPem(c);
            break;
        }
       final MinimalTestVitamApplicationFactory<TestVitamApplication> testFactory =
            new MinimalTestVitamApplicationFactory<TestVitamApplication>() {

                @Override
                public StartApplicationResponse<TestVitamApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    final TestVitamApplication application = new TestVitamApplication(INGEST_EXTERNAL_CONF);
                    final StartApplicationResponse<TestVitamApplication> response = startAndReturn(application);
                    return response;
                }

            };
        final StartApplicationResponse<TestVitamApplication> response = testFactory.findAvailablePortSetToApplication();
        serverPort = response.getServerPort();
        application = response.getApplication();
        LOGGER.warn("Start configuration: " + serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        JunitHelper.getInstance().releasePort(serverPort);
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
    public void givenHttpCallWithoutHeaderCertificateThenRaizeShiroException() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
        //configuration.setServerPort(serverPort);

        final VitamClientFactory<DefaultClient> factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        //factory.disableUseAuthorizationFilter();

        if (application.getVitamServer().isStarted()) {
            try (final DefaultClient client = factory.getClient()) {
                client.checkStatus();
                LOGGER.error("THIS SHOULD RAIZED AN EXCEPTION");
                fail("THIS SHOULD RAIZED EXCEPTION");
            } catch (final VitamException e) {
            }
        }
    }



    /**
     * Test passing certificate a valid role in the header and assert that ok
     */
    @Test
    public void givenHttpCallWithHeaderCertificateThenOK() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
        //configuration.setServerPort(serverPort);

        final VitamClientFactory<DefaultClient> factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        //factory.disableUseAuthorizationFilter();

        if (application.getVitamServer().isStarted()) {
            try (final DefaultClient client = factory.getClient()) {

                MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
                List<Object> objectList = new ArrayList<>();
                objectList.add(pem);

                headers.put(X509AuthenticationFilter.X_SSL_CLIENT_CERT, objectList);
                client.checkStatus(headers);
            } catch (final VitamException e) {
                LOGGER.error("THIS SHOULD NOT RAIZED AN EXCEPTION", e);
                fail("THIS SHOULD NOT RAIZED AN EXCEPTION");
            }
        }
    }



    /**
     * Test passing certificate expired in the header and assert that throw exception
     */
    @Test
    public void givenHttpCallWithHeaderCertificateExpiredThenRaiseAnException() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
        //configuration.setServerPort(serverPort);

        final VitamClientFactory<DefaultClient> factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        //factory.disableUseAuthorizationFilter();

        if (application.getVitamServer().isStarted()) {
            try (final DefaultClient client = factory.getClient()) {

                MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
                List<Object> objectList = new ArrayList<>();
                objectList.add(pemExpired);

                headers.put(X509AuthenticationFilter.X_SSL_CLIENT_CERT, objectList);
                client.checkStatus(headers);
                fail("SHould Raized an exception");
            } catch (final VitamException e) {
            }
        }
    }

    /**
     * Test passing certificate not granted role in the header and assert that throw exception
     */
    @Test
    public void givenHttpCallWithHeaderCertificateNotGrantedThenReturnForbidden() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
        //configuration.setServerPort(serverPort);

        final VitamClientFactory<DefaultClient> factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        //factory.disableUseAuthorizationFilter();

        if (application.getVitamServer().isStarted()) {
            try (final DefaultClient client = factory.getClient()) {

                MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
                List<Object> objectList = new ArrayList<>();
                objectList.add(pemNotGranted);

                headers.put(X509AuthenticationFilter.X_SSL_CLIENT_CERT, objectList);
                client.checkStatus(headers);
                fail("SHould Raized an exception");
            } catch (final VitamException e) {
            }
        }
    }
}