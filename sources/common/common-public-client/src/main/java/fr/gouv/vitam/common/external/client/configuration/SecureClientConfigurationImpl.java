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
package fr.gouv.vitam.common.external.client.configuration;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * SecureClientConfiguration interface's implementation
 */
public class SecureClientConfigurationImpl extends ClientConfigurationImpl implements SecureClientConfiguration {

    private static final String CONFIGURATION_PARAMETERS = "SecureClientConfiguration parameters";
    private boolean secure = true;
    private SSLConfiguration sslConfiguration = new SSLConfiguration();
    private boolean hostnameVerification = true;

    /**
     * Empty SecureClientConfiguration constructor for YAMLFactory
     */
    public SecureClientConfigurationImpl() {
        // nothing
    }

    /**
     * ClientConfiguration constructor
     *
     * @param serverHost server IP address
     * @param serverPort server port
     * @throws IllegalArgumentException if serverHost is null or empty or serverPort &lt;= 0
     */
    public SecureClientConfigurationImpl(String serverHost, int serverPort) {
        this(serverHost, serverPort, true, new SSLConfiguration(), true);
    }

    /**
     * ClientConfiguration constructor
     *
     * @param serverHost server IP address
     * @param serverPort server port
     * @param secure HTTP/HTTPS
     * @throws IllegalArgumentException if configuration param is null or empty or serverPort &lt;= 0
     */
    public SecureClientConfigurationImpl(String serverHost, int serverPort, boolean secure) {
        this(serverHost, serverPort, secure, new SSLConfiguration(), true);
    }

    /**
     * ClientConfiguration constructor
     *
     * @param serverHost server IP address
     * @param serverPort server port
     * @param secure HTTP/HTTPS
     * @param sslConfiguration
     * @throws IllegalArgumentException if configuration param is null or empty or serverPort &lt;= 0
     */
    public SecureClientConfigurationImpl(
        String serverHost,
        int serverPort,
        boolean secure,
        SSLConfiguration sslConfiguration
    ) {
        this(serverHost, serverPort, secure, sslConfiguration, true);
    }

    /**
     * ClientConfiguration constructor
     *
     * @param serverHost server IP address
     * @param serverPort server port
     * @param secure
     * @param sslConfiguration
     * @param hostnameVerification
     * @throws IllegalArgumentException if any configuration param is null or empty or serverPort &lt;= 0
     */
    public SecureClientConfigurationImpl(
        String serverHost,
        int serverPort,
        boolean secure,
        SSLConfiguration sslConfiguration,
        boolean hostnameVerification
    ) {
        super(serverHost, serverPort);
        this.secure = secure;
        this.hostnameVerification = hostnameVerification;
        this.sslConfiguration = sslConfiguration;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public boolean isHostnameVerification() {
        return hostnameVerification;
    }

    @Override
    public SSLConfiguration getSslConfiguration() {
        return sslConfiguration;
    }

    /**
     * @param hostnameVerification the hostnameVerification to set
     * @return this
     */
    public SecureClientConfigurationImpl setHostnameVerification(boolean hostnameVerification) {
        this.hostnameVerification = hostnameVerification;
        return this;
    }

    /**
     * @param secure the secure to set
     * @return this
     */
    public SecureClientConfigurationImpl setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * @param sslConfiguration the sslConfiguration to set
     * @return this
     */
    public SecureClientConfigurationImpl setSslConfiguration(SSLConfiguration sslConfiguration) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, sslConfiguration);
        this.sslConfiguration = sslConfiguration;
        return this;
    }
}
