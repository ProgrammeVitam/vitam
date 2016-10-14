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
package fr.gouv.vitam.common.client;

import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusMessage;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;

/**
 * Abstract client class for all vitam client not using SSL
 */
public abstract class AbstractClient implements BasicClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractClient.class);

    protected static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private final String serviceUrl;
    private String resourcePath;
    private final Client client;
    private final ClientConfigurationImpl clientConfiguration;

    /**
     * Constructor using given scheme (http)
     *
     * @param configuration The client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @param suppressHttpCompliance define if client (Jetty Client feature) check if request id HTTP compliant
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected AbstractClient(ClientConfigurationImpl configuration, String resourcePath,
        boolean suppressHttpCompliance) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        ParametersChecker.checkParameter("resourcePath cannot be null", resourcePath);
        final String scheme = "http";

        ParametersChecker.checkParameter("Host cannot be null", configuration.getServerHost());
        ParametersChecker.checkValue("Port has invalid value", configuration.getServerPort(), 1);
        clientConfiguration = configuration;

        this.resourcePath = Optional.ofNullable(resourcePath).orElse("/");

        this.resourcePath = resourcePath;
        final String uri = this.resourcePath;

        serviceUrl =
            scheme + "://" + clientConfiguration.getServerHost() + ":" + clientConfiguration.getServerPort() + uri;
        final ClientConfig config = configure(suppressHttpCompliance);
        client = ClientBuilder.newClient(config);
    }

    /**
     * ONLY use this constructor in unit test Remove this when JerseyTest will be fully compatible with Jetty
     *
     * @param configuration the client configuration
     * @param resourcePath the resource path of the server for the client calls
     * @param client the HTTP client to use
     * @throws UnsupportedOperationException HTTPS not implemented yet
     */
    protected AbstractClient(ClientConfigurationImpl configuration, String resourcePath, Client client) {
        ParametersChecker.checkParameter("Jersey client", client);
        ParametersChecker.checkParameter("Configuration cannot be null ", configuration);
        ParametersChecker.checkParameter("Context path cannot be null ", configuration.getServerHost());
        ParametersChecker.checkParameter("ResourcePath cannot be null", resourcePath);
        ParametersChecker.checkValue("port", configuration.getServerPort(), 1);
        clientConfiguration = configuration;

        this.resourcePath = resourcePath;
        final String uri = this.resourcePath;

        serviceUrl = "http://" + clientConfiguration.getServerHost() + ":" + clientConfiguration.getServerPort() + uri;
        this.client = client;
    }

    /**
     * Common method to handle status responses
     *
     * @param response the JAX-RS response from the server
     * @param responseType the type to map the response into
     * @param <R> response type parameter
     * @return the Response mapped as an POJO
     * @throws VitamClientException the exception if any from the server
     */
    protected abstract <R> R handleCommonResponseStatus(Response response, Class<R> responseType)
        throws VitamClientException;

    protected ClientConfig configure(boolean suppressHttpCompliance) {
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, suppressHttpCompliance);
        config.connectorProvider(new JettyConnectorProvider());
        return config;
    }

    @Override
    public StatusMessage getStatus() throws VitamClientException {
        Response response = null;
        try {
            response = client.target(serviceUrl).path(STATUS_URL).request().get();
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    StatusMessage message;
                    if (response.hasEntity()) {
                        message = response.readEntity(StatusMessage.class);
                    } else {
                        message = new StatusMessage();
                    }
                    return message;
                case NO_CONTENT:
                    return new StatusMessage();
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new VitamClientException(INTERNAL_SERVER_ERROR);
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    /**
     * Perform a HTTP request to the server for the simple use cases
     *
     * @param path URL to request
     * @param query query DSL
     * @param accept asked type of response
     * @param headers headers HTTP to add to request
     * @param httpMethod HTTP method to use for request
     * @param contentType the media type of the entity to send
     * @return the reponse from the server
     */
    protected Response performGenericRequest(String path, Object query, String accept,
        MultivaluedHashMap<String, Object> headers, String httpMethod, String contentType) {
        return getClient().target(getServiceUrl()).path(path).request(MediaType.APPLICATION_JSON)
            .headers(headers).accept(accept).method(httpMethod, Entity.entity(query, contentType));
    }

    /**
     * Get the service URL
     *
     * @return the service URL
     */
    protected String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Get the client
     *
     * @return the client
     */
    protected Client getClient() {
        return client;
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }
}
