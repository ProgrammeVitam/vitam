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

package fr.gouv.vitamui.antivirus.client;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.HeaderIdClientFilter;
import fr.gouv.vitam.common.client.VitamApacheHttpClientEngine;
import fr.gouv.vitam.common.client.VitamRestEasyConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.FileNotFoundException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for client factories for OpenAPI clients in Vitam. This is heavily inspired by the VitamClientFactory class.
 */
public final class VitamOpenAPIClientFactoryUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamOpenAPIClientFactoryUtils.class);

    /**
     * Specific Socket Configuration
     */
    private static final SocketConfig SOCKETCONFIG = SocketConfig.custom()
        .setRcvBufSize(VitamConfiguration.getRecvBufferSize())
        .setSndBufSize(0)
        .setSoKeepAlive(true)
        .setSoReuseAddress(true)
        .setTcpNoDelay(true)
        .setSoTimeout(VitamConfiguration.getReadTimeout())
        .build();

    private static void initConfig(Map<VitamRestEasyConfiguration, Object> config) {
        config.put(VitamRestEasyConfiguration.CONNECT_TIMEOUT, VitamConfiguration.getConnectTimeout());
        config.put(VitamRestEasyConfiguration.CONNECTTIMEOUT, VitamConfiguration.getConnectTimeout());
        config.put(VitamRestEasyConfiguration.CONNECTIONREQUESTTIMEOUT, VitamConfiguration.getDelayGetClient());
        config.put(VitamRestEasyConfiguration.READ_TIMEOUT, VitamConfiguration.getReadTimeout());
        config.put(VitamRestEasyConfiguration.SOCKETTIMEOUT, VitamConfiguration.getReadTimeout());
        config.put(VitamRestEasyConfiguration.CONTENTCOMPRESSIONENABLED, VitamConfiguration.isAllowGzipEncoding());
        config.put(VitamRestEasyConfiguration.CHUNKED_ENCODING_SIZE, 0);
        config.put(VitamRestEasyConfiguration.REQUEST_ENTITY_PROCESSING, VitamRestEasyConfiguration.BUFFERED);
        config.put(VitamRestEasyConfiguration.CACHE_ENABLED, false);
        config.put(VitamRestEasyConfiguration.RECV_BUFFER_SIZE, VitamConfiguration.getRecvBufferSize());
        config.put(VitamRestEasyConfiguration.CONNECTION_MANAGER_SHARED, true);
        config.put(VitamRestEasyConfiguration.DISABLE_AUTOMATIC_RETRIES, true);
        config.put(
            VitamRestEasyConfiguration.REQUEST_CONFIG,
            RequestConfig.custom().setConnectionRequestTimeout(VitamConfiguration.getDelayGetClient()).build()
        );
    }

    public static Client buildHttpClient(ClientConfiguration configuration) {
        // Build the http client configuration
        Map<VitamRestEasyConfiguration, Object> configMap = new EnumMap<>(VitamRestEasyConfiguration.class);
        VitamOpenAPIClientFactoryUtils.initConfig(configMap);
        if (configuration.isSecure()) {
            // SSL configuration
            final SecureClientConfiguration secureClientConfiguration = (SecureClientConfiguration) configuration;
            SSLConfiguration sslConfiguration = secureClientConfiguration.getSslConfiguration();
            ParametersChecker.checkParameter("sslConfiguration is a mandatory parameter", sslConfiguration);
            Registry<ConnectionSocketFactory> registry;
            try {
                SSLContext context = sslConfiguration.createSSLContext();
                configMap.put(VitamRestEasyConfiguration.SSL_CONTEXT, context);
                registry = sslConfiguration.getRegistry(context);
            } catch (FileNotFoundException | VitamException e) {
                LOGGER.error(e);
                throw new IllegalArgumentException("SSLConfiguration issue while reading KeyStore or TrustStore", e);
            }
            PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(
                registry,
                null,
                null,
                null,
                VitamConfiguration.getMaxDelayUnusedConnection(),
                TimeUnit.MILLISECONDS
            );
            // Set up the connection pool
            pool.setMaxTotal(VitamConfiguration.getMaxTotalClient());
            pool.setDefaultMaxPerRoute(VitamConfiguration.getMaxClientPerHost());
            pool.setValidateAfterInactivity(VitamConfiguration.getDelayValidationAfterInactivity());
            pool.setDefaultSocketConfig(SOCKETCONFIG);
            configMap.put(VitamRestEasyConfiguration.CONNECTION_MANAGER, pool);
        } else {
            configMap.put(
                VitamRestEasyConfiguration.CONNECTION_MANAGER,
                new PoolingHttpClientConnectionManager(
                    VitamConfiguration.getMaxDelayUnusedConnection(),
                    TimeUnit.MILLISECONDS
                )
            );
        }
        // Build the http client from these parameters
        VitamApacheHttpClientEngine engine = new VitamApacheHttpClientEngine(configMap);
        ResteasyClientBuilder builder = configureRestEasy(configMap, engine);
        return builder.build();
    }

    private static ResteasyClientBuilder configureRestEasy(
        Map<VitamRestEasyConfiguration, Object> config,
        ClientHttpEngine engine
    ) {
        ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
        clientBuilder.httpEngine(engine);
        clientBuilder.connectionCheckoutTimeout(
            VitamRestEasyConfiguration.CONNECTIONREQUESTTIMEOUT.getInt(config, 1000),
            TimeUnit.MILLISECONDS
        );
        clientBuilder.connectTimeout(
            VitamRestEasyConfiguration.CONNECT_TIMEOUT.getInt(config, 1000),
            TimeUnit.MILLISECONDS
        );
        clientBuilder.readTimeout(
            VitamRestEasyConfiguration.READ_TIMEOUT.getInt(config, 100000),
            TimeUnit.MILLISECONDS
        );
        clientBuilder.executorService(VitamThreadPoolExecutor.getDefaultExecutor());
        if (VitamConfiguration.isAllowGzipDecoding()) {
            clientBuilder.register(AcceptEncodingGZIPFilter.class);
            clientBuilder.register(GZIPDecodingInterceptor.class);
        }
        if (VitamConfiguration.isAllowGzipEncoding()) {
            clientBuilder.register(GZIPEncodingInterceptor.class);
        }
        // Jackson automatically included
        // Always use authorization filter
        clientBuilder.register(HeaderIdClientFilter.class);
        return clientBuilder;
    }
}
