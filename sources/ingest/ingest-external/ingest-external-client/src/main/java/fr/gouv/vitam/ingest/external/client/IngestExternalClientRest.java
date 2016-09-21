package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;
import java.net.SocketException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamRestClientBuilder;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.SSLConfiguration;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.client.ErrorMessage;

/**
 * Ingest External client
 */
public class IngestExternalClientRest implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientRest.class);
    private static final String RESOURCE_PATH = "/ingest-ext/v1";
    private static final String UPLOAD_URL = "/upload";
    private static final String STATUS = "/status";

    private String serviceUrl = "http";
    private final Client client;


    /**
     * Constructor IngestExternalClientRest
     * 
     * @param server
     * @param port
     * @throws VitamException
     */
    IngestExternalClientRest(String server, int port) throws VitamException {
        this(server, port, true, new SSLConfiguration(), true);

    }

    /**
     * Constructor IngestExternalClientRest
     * 
     * @param server
     * @param port
     * @throws VitamException
     */
    IngestExternalClientRest(String server, int port, boolean secure, SSLConfiguration sslConfiguration,
        boolean hostnameVerification) throws VitamException {
        ParametersChecker.checkParameter("server and port are a mandatory parameter", server, port, secure,
            sslConfiguration, hostnameVerification);
        if (secure) {
            serviceUrl = "https";
        }
        serviceUrl += "://" + server + ":" + port + RESOURCE_PATH;

        VitamRestClientBuilder restClientBuilder = new VitamRestClientBuilder();
        client = restClientBuilder
            .setSslConfiguration(sslConfiguration)
            .setHostnameVerification(hostnameVerification)
            .build();


    }

    @Override
    public void upload(InputStream stream) throws IngestExternalException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        try {
            final Response response = client.target(serviceUrl).path(UPLOAD_URL)
                .request(MediaType.APPLICATION_OCTET_STREAM)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
            String result = response.readEntity(String.class);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    throw new IngestExternalException(result);
                default:
                    throw new IngestExternalException("Unknown error");
            }
        } catch (ProcessingException e) {
            if (e.getCause().getClass().equals(SocketException.class)) {
                throw new IngestExternalException("Exception can linked to SSL", e);
            } else {
                throw new IngestExternalException(e.getMessage(), e);
            }
        }
    }

    @Override
    public Status status() throws IngestExternalException {
        try {
            final Response response = client.target(serviceUrl).path(STATUS).request().get();
            return Status.fromStatusCode(response.getStatus());

        } catch (ProcessingException e) {
            if (e.getCause().getClass().equals(SocketException.class)) {
                throw new IngestExternalException("Exception can linked to SSL", e);
            } else {
                throw new IngestExternalException(e.getMessage(), e);
            }
        }
    }
}
