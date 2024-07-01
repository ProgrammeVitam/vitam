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
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;

public class DefaultSslHeaderClientTest extends ResteasyTestApplication {

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultSslHeaderClientTest.class);

    private static final String BASE_URI = "/ingest-ext/v1";
    private static final String INGEST_EXTERNAL_CLIENT_CONF_NOKEY = "standard-client-secure_nokey.conf";

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        DefaultSslHeaderClientTest.class,
        false,
        false,
        false,
        true
    );

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new SslResource());
    }

    private static String pem = null;
    private static String pemExpired = null;
    private static String pemNotGranted = null;

    @Path(BASE_URI)
    @javax.ws.rs.ApplicationPath("webresources")
    private static class SslResource extends ApplicationStatusResource {
        // Empty
    }

    private static String x509CertificateToPemWithWiteSpaceApacheFormat(X509Certificate cert)
        throws CertificateEncodingException {
        StringWriter sw = new StringWriter();
        sw.write(BEGIN_CERT);
        sw.write(" ");
        sw.write(Base64.getEncoder().encodeToString(cert.getEncoded()));
        sw.write(" ");
        sw.write(END_CERT);
        return sw.toString().replaceAll("\n", " ");
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();

        KeyStore p12 = KeyStore.getInstance("pkcs12");
        p12.load(PropertiesUtils.getConfigAsStream("tls/client/client.p12"), "azerty4".toCharArray());
        Enumeration e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            pem = x509CertificateToPemWithWiteSpaceApacheFormat(c);
            break;
        }

        p12.load(PropertiesUtils.getConfigAsStream("tls/client/client_expired.p12"), "vitam2016".toCharArray());
        e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            pemExpired = x509CertificateToPemWithWiteSpaceApacheFormat(c);
            break;
        }

        p12.load(PropertiesUtils.getConfigAsStream("tls/client/client_notgranted.p12"), "vitam2016".toCharArray());
        e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            pemNotGranted = x509CertificateToPemWithWiteSpaceApacheFormat(c);
            break;
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
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
    public void givenHttpCallWithoutHeaderCertificateThenRaizeShiroException() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
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
            LOGGER.error("THIS SHOULD RAIZED AN EXCEPTION");
            fail("THIS SHOULD NOT RAIZED EXCEPTION");
        } catch (final VitamException e) {} finally {
            try {
                factory.shutdown();
            } catch (Exception e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    /**
     * Test passing certificate a valid role in the header and assert that ok
     */
    @Test
    public void givenHttpCallWithHeaderCertificateThenOK() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
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
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            List<Object> objectList = new ArrayList<>();
            objectList.add(pem);

            headers.put(GlobalDataRest.X_SSL_CLIENT_CERT, objectList);
            client.checkStatus(headers);
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

    /**
     * Test passing certificate expired in the header and assert that throw exception
     */
    @Test
    public void givenHttpCallWithHeaderCertificateExpiredThenRaiseAnException() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
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
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            List<Object> objectList = new ArrayList<>();
            objectList.add(pemExpired);

            headers.put(GlobalDataRest.X_SSL_CLIENT_CERT, objectList);
            client.checkStatus(headers);
            fail("SHould Raized an exception");
        } catch (final VitamException e) {} finally {
            try {
                factory.shutdown();
            } catch (Exception e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    /**
     * Test passing certificate not granted role in the header and assert that throw exception
     */
    @Test
    public void givenHttpCallWithHeaderCertificateNotGrantedThenReturnForbidden() {
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF_NOKEY);
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
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            List<Object> objectList = new ArrayList<>();
            objectList.add(pemNotGranted);

            headers.put(GlobalDataRest.X_SSL_CLIENT_CERT, objectList);
            client.checkStatus(headers);
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
