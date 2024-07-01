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
package fr.gouv.vitam.common.external.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.external.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.external.client.configuration.SSLKey;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.SslConfig;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DefaultSslClientTest extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultSslClientTest.class);

    private static final String BASE_URI = "/ingest-ext/v1";
    private static final String INGEST_EXTERNAL_CLIENT_CONF = "standard-client-secure.conf";
    private static final String INGEST_EXTERNAL_SERVER_CONF = "standard-server-secure.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_NOTGRANTED = "standard-client-secure_notgranted.conf";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_EXPIRED = "standard-client-secure_expired.conf";

    private static final SecureClientConfiguration configurationServer = changeConfigurationFile(
        INGEST_EXTERNAL_SERVER_CONF
    );

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        DefaultSslClientTest.class,
        VitamServerTestRunner.AdminApp.class,
        new SslConfig(
            configurationServer.getSslConfiguration().getKeystore().iterator().next().getKeyPath(),
            configurationServer.getSslConfiguration().getKeystore().iterator().next().getKeyPassword(),
            configurationServer.getSslConfiguration().getTruststore().iterator().next().getKeyPath(),
            configurationServer.getSslConfiguration().getTruststore().iterator().next().getKeyPassword()
        ),
        null,
        false,
        false,
        false,
        true,
        false
    );

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new SslResource());
    }

    @Path(BASE_URI)
    @javax.ws.rs.ApplicationPath("webresources")
    private static class SslResource extends ApplicationStatusResource {
        // Empty
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Test
    public void testClientBuilder() throws Exception {
        final SSLKey key = new SSLKey("tls/client/client.p12", "azerty4");
        final ArrayList<SSLKey> truststore = new ArrayList<>();
        truststore.add(key);
        final SSLConfiguration sslConfig = new SSLConfiguration(truststore, truststore);
        final SecureClientConfiguration configuration = new SecureClientConfigurationImpl(
            "host",
            8443,
            true,
            sslConfig,
            false
        );
        final VitamClientFactory<DefaultClient> factory = new VitamClientFactory<DefaultClient>(
            configuration,
            BASE_URI
        ) {
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
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(configurationPath),
                SecureClientConfigurationImpl.class
            );
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
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF);
        configuration.setServerPort(vitamServerTestRunner.getBusinessPort());

        final VitamClientFactory<DefaultClient> factory = new VitamClientFactory<DefaultClient>(
            configuration,
            BASE_URI
        ) {
            @Override
            public DefaultClient getClient() {
                return new DefaultClient(this);
            }
        };
        LOGGER.warn("Start Client configuration: " + factory);
        try (final DefaultClient client = factory.getClient()) {
            client.checkStatus();
        } catch (final VitamException e) {
            LOGGER.error("THIS SHOULD NOT RAIZED AN EXCEPTION", e);
            fail("THIS SHOULD NOT RAIZED AN EXCEPTION");
        } finally {
            try {
                factory.shutdown();
            } catch (Exception e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    @Test
    public void givenCertifNotGrantedThenReturnForbidden() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOTGRANTED);
        configuration.setServerPort(vitamServerTestRunner.getBusinessPort());

        final VitamClientFactory<DefaultClient> factory = new VitamClientFactory<DefaultClient>(
            configuration,
            BASE_URI
        ) {
            @Override
            public DefaultClient getClient() {
                return new DefaultClient(this);
            }
        };
        try (final DefaultClient client = factory.getClient()) {
            client.checkStatus();
            fail("Should Raized an exception");
        } catch (final VitamException e) {} finally {
            try {
                factory.shutdown();
            } catch (Exception e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    @Test
    public void givenCertifExpiredThenRaiseAnException() throws VitamException {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_EXPIRED);
        configuration.setServerPort(vitamServerTestRunner.getBusinessPort());

        final VitamClientFactory<DefaultClient> factory = new VitamClientFactory<DefaultClient>(
            configuration,
            BASE_URI
        ) {
            @Override
            public DefaultClient getClient() {
                return new DefaultClient(this);
            }
        };
        try (final DefaultClient client = factory.getClient()) {
            client.checkStatus();
            fail("SHould Raized an exception");
        } catch (final VitamException e) {} finally {
            try {
                factory.shutdown();
            } catch (Exception e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }
}
