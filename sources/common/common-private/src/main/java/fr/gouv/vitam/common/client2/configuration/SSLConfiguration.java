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
package fr.gouv.vitam.common.client2.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.glassfish.jersey.SslConfigurator;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;

/**
 * SSL Configuration
 */
public class SSLConfiguration {

    private static final String PARAMETERS = "SSLConfiguration parameters";
    private static final AllowAllHostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = new AllowAllHostnameVerifier();
    private List<SSLKey> truststore;
    private List<SSLKey> keystore;

    /**
     * Empty SSLConfiguration constructor for YAMLFactory
     */

    public SSLConfiguration() {
        // Empty
    }

    /**
     * SSLConfiguration Constructor
     * 
     * @param keystore
     * @param truststore
     * @throws IllegalArgumentException if keystore/truststore is null or empty
     */
    public SSLConfiguration(List<SSLKey> keystore, List<SSLKey> truststore) {
        ParametersChecker.checkParameter(PARAMETERS, truststore, keystore);
        this.truststore = truststore;
        this.keystore = keystore;
    }

    private String getTrustFile() throws FileNotFoundException {
        SSLKey key = truststore.get(0);
        File file = PropertiesUtils.findFile(key.getKeyPath());
        return file.getAbsolutePath();
    }

    private String getKeyFile() throws FileNotFoundException {
        SSLKey key = keystore.get(0);
        File file = PropertiesUtils.findFile(key.getKeyPath());
        return file.getAbsolutePath();
    }

    /**
     * 
     * @return the associate SslConfigurator
     * @throws FileNotFoundException
     */
    // FIXME see later if multiple Keystores/TrustStores are necessary
    public SslConfigurator createSslConfigurator() throws FileNotFoundException {
        SslConfigurator configurator = SslConfigurator.newInstance();
        if (truststore != null) {
            SSLKey key = truststore.get(0);
            configurator.trustStoreFile(getTrustFile()).trustStorePassword(key.getKeyPassword());
        }
        if (keystore != null) {
            SSLKey key = keystore.get(0);
            configurator.keyStoreFile(getKeyFile()).keyPassword(key.getKeyPassword());
        }
        return configurator;
    }

    /**
     * 
     * @return the associate Registry for Apache Ssl configuration
     * @throws FileNotFoundException
     */
    public Registry<ConnectionSocketFactory> getRegistry() throws FileNotFoundException {
        SslConfigurator sslConfigurator = createSslConfigurator();
        return RegistryBuilder.<ConnectionSocketFactory>create()
            .register("https", new SSLConnectionSocketFactory(sslConfigurator.createSSLContext(),
                getAllowAllHostnameVerifier())) // force
            .build();
    }

    /**
     * @return the truststore
     */
    public List<SSLKey> getTruststore() {
        return truststore;
    }

    /**
     * @return the keystore
     */
    public List<SSLKey> getKeystore() {
        return keystore;
    }

    /**
     * @param truststore the truststore to set
     *
     * @return this
     */
    public SSLConfiguration setTruststore(List<SSLKey> truststore) {
        this.truststore = truststore;
        return this;
    }

    /**
     * @param keystore the keystore to set
     *
     * @return this
     */
    public SSLConfiguration setKeystore(List<SSLKey> keystore) {
        this.keystore = keystore;
        return this;
    }

    /**
     * * @param verification * @return HostnameVerifier : An Allow All HostNameVerifier
     */
    private HostnameVerifier getAllowAllHostnameVerifier() {
        return ALLOW_ALL_HOSTNAME_VERIFIER;
    }
    
    private static class AllowAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            return true;
        }
    }
}
