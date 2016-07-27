package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.client.ErrorMessage;

/**
 *  Ingest External client
 */
public class IngestExternalClientRest implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientRest.class);
    private static final String RESOURCE_PATH = "/ingest-ext/v1";
    private static final String UPLOAD_URL = "/upload";
    private static final String STATUS = "/status";

    private final String serviceUrl;
    private final Client client;

    /**
     * Constructor IngestExternalClientRest
     * 
     * @param server
     * @param port
     */
    IngestExternalClientRest(String server, int port) {
        ParametersChecker.checkParameter("server is a mandatory parameter", server);
        ParametersChecker.checkParameter("port is a mandatory parameter", port);
        serviceUrl = "http://" + server + ":" + port + RESOURCE_PATH;
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
    }
    
    @Override
    public void upload(InputStream stream) throws IngestExternalException{
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        final Response response = client.target(serviceUrl).path(UPLOAD_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                break;
            case ACCEPTED:
                LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                throw new IngestExternalException("upload is ongoing or error");    
        }
    }
    
    @Override
    public Status status(){
        final Response response = client.target(serviceUrl).path(STATUS).request().get();
        return Status.fromStatusCode(response.getStatus());
    }

}
