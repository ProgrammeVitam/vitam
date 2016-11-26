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

import javax.ws.rs.client.Client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.netty.connector.NettyConnectorProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.server.benchmark.BenchmarkConnectorProvider;

/**
 * Abstract client class for all vitam client not using SSL
 */
public abstract class AbstractBenchmarkClientFactory<T extends BasicClient> extends TestVitamClientFactory<T> {

    /**
     * Connector Provider : connector used
     */
    private BenchmarkConnectorProvider benchmarkConnectorProvider;

    /**
     * Constructor with standard configuration
     *
     * @param serverPort
     * @param resourcePath the resource path of the server for the client calls
     * @param suppressHttpCompliance define if client (Jetty Client feature) check if request id HTTP compliant
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected AbstractBenchmarkClientFactory(int serverPort, String resourcePath) {
        super(serverPort, resourcePath);
    }

    /**
     * Constructor to allow to enable Multipart support (until all are removed)
     *
     * @param serverPort
     * @param resourcePath the resource path of the server for the client calls
     * @param suppressHttpCompliance define if client (Jetty Client feature) check if request id HTTP compliant
     * @param multipart allow multipart and disabling chunked mode
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected AbstractBenchmarkClientFactory(int serverPort, String resourcePath,
        boolean allowMultipart) {
        super(serverPort, resourcePath, allowMultipart);
    }

    /**
     * ONLY use this constructor in unit test Remove this when JerseyTest will be fully compatible with Jetty
     *
     * @param serverPort
     * @param resourcePath the resource path of the server for the client calls
     * @param client the HTTP client to use
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected AbstractBenchmarkClientFactory(int serverPort, String resourcePath, Client client) {
        super(serverPort, resourcePath, client);
    }

    @Override
    void internalConfigure() {
        // Default value
        benchmarkConnectorProvider = BenchmarkConnectorProvider.APACHE;
        startup();
        benchmarkConfigure(config, true);
        benchmarkConfigure(configNotChunked, false);
    }

    /**
     * Replace internalConfigure(ClientConfig, boolean)
     *
     * @param config
     * @param chunkedMode
     */
    private void benchmarkConfigure(ClientConfig config, boolean chunkedMode) {
        commonConfigure(config);
        internalConfigure(config, chunkedMode);
    }

    /**
     * Change the underlying Connector Provider. Equivalent to call the constructor with new ConnectorProvider
     *
     * @param mode
     * @param chunkedMode
     */
    public void mode(BenchmarkConnectorProvider mode) {
        ParametersChecker.checkParameter("ConnectorProvider must not be null", mode);
        benchmarkConnectorProvider = mode;
        internalConfigure(config, true);
        internalConfigure(configNotChunked, false);
    }

    /**
     *
     * @return the current underlying Connector Provider
     */
    public BenchmarkConnectorProvider getMode() {
        return benchmarkConnectorProvider;
    }

    private void internalConfigure(ClientConfig config, boolean chunkedMode) {
        switch (benchmarkConnectorProvider) {
            case APACHE:
                commonApacheConfigure(config, chunkedMode);
                POOLING_CONNECTION_MANAGER
                    .setValidateAfterInactivity(VitamConfiguration.getDelayValidationAfterInactivity());
                break;
            case APACHE_NOCHECK:
                commonApacheConfigure(config, chunkedMode);
                POOLING_CONNECTION_MANAGER
                    .setValidateAfterInactivity(VitamConfiguration.NO_VALIDATION_AFTER_INACTIVITY);
                break;
            case GRIZZLY:
                config.connectorProvider(new GrizzlyConnectorProvider());
                break;
            case NETTY:
                config.connectorProvider(new NettyConnectorProvider());
                break;
            case STANDARD:
            default:
                break;
        }
    }
}
