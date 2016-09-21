package fr.gouv.vitam.functional.administration.client;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * AdminManagement client
 */
public class AdminManagementClientRest implements AdminManagementClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementClientRest.class);
    private static final String RESOURCE_PATH = "/adminmanagement/v1";
    private static final String FORMAT_CHECK_URL = "/format/check";
    private static final String FORMAT_IMPORT_URL = "/format/import";
    private static final String FORMAT_DELETE_URL = "/format/delete";
    private static final String FORMAT_GET_DOCUMENT_URL = "/format/document";
    private static final String FORMAT_URL = "/format";

    private static final String RULESMANAGER_CHECK_URL = "/rules/check";
    private static final String RULESMANAGER_IMPORT_URL = "/rules/import";
    private static final String RULESMANAGER_DELETE_URL = "/rules/delete";
    private static final String RULESMANAGER_GET_DOCUMENT_URL = "/rules/document";
    private static final String RULESMANAGER_URL = "/rules";

    private static final String STATUS = "/status";

    private final String serviceUrl;
    private final Client client;

    /**
     * Constructor IngestExternalClientRest
     * 
     * @param server
     * @param port
     */
    AdminManagementClientRest(String server, int port) {
        ParametersChecker.checkParameter("server is a mandatory parameter", server);
        ParametersChecker.checkParameter("port is a mandatory parameter", port);
        serviceUrl = "http://" + server + ":" + port + RESOURCE_PATH;
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
    }

    @Override
    public Status status() {
        final Response response = client.target(serviceUrl).path(STATUS).request().get();
        return Status.fromStatusCode(response.getStatus());
    }

    // TODO : Refactorisation à réfléchir pour ne pas avoir une seule classe gérant tous les endpoints (formats, régles
    // de gestions, contrat , etc)
    @Override
    public Status checkFormat(InputStream stream) throws ReferentialException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        final Response response = client.target(serviceUrl).path(FORMAT_CHECK_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case PRECONDITION_FAILED:
                LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new ReferentialException("File format error");
            default:
                break;
        }
        return status;
    }

    @Override
    public void importFormat(InputStream stream) throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        final Response response = client.target(serviceUrl).path(FORMAT_IMPORT_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case PRECONDITION_FAILED:
                LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new ReferentialException("File format error");
            case CONFLICT:
                LOGGER.debug(Response.Status.CONFLICT.getReasonPhrase());
                throw new DatabaseConflictException("Collection input conflic");
            default:
                break;
        }
    }

    @Override
    public void deleteFormat() throws ReferentialException {
        final Response response = client.target(serviceUrl).path(FORMAT_DELETE_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .delete(Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case PRECONDITION_FAILED:
                LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new ReferentialException("File format error");
            default:
                break;
        }
    }

    @Override
    public JsonNode getFormatByID(String id) throws ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);

        final Response response = client.target(serviceUrl).path(FORMAT_URL + "/" + id)
            .request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(id, MediaType.APPLICATION_JSON), Response.class);

        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case NOT_FOUND:
                LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                throw new ReferentialException("Rules Not found ");
            default:
                break;
        }

        return JsonHandler.getFromString(response.readEntity(String.class));
    }

    @Override
    public JsonNode getFormats(JsonNode query) throws ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        final Response response = client.target(serviceUrl).path(FORMAT_GET_DOCUMENT_URL)
            .request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(query, MediaType.APPLICATION_JSON), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case NOT_FOUND:
                LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                throw new ReferentialException("File format error");
            default:
                break;
        }
        return JsonHandler.getFromString(response.readEntity(String.class));
    }

    /************************** rules Management ****************************************/

    @Override
    public Status checkRulesFile(InputStream stream) throws FileRulesException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        final Response response = client.target(serviceUrl).path(RULESMANAGER_CHECK_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case PRECONDITION_FAILED:
                LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new FileRulesException("File rules error");
            default:
                break;
        }
        return status;
    }

    @Override
    public void importRulesFile(InputStream stream) throws FileRulesException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        final Response response = client.target(serviceUrl).path(RULESMANAGER_IMPORT_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case PRECONDITION_FAILED:
                LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new FileRulesException("File rules error");
            case CONFLICT:
                LOGGER.debug(Response.Status.CONFLICT.getReasonPhrase());
                throw new DatabaseConflictException("Collection input conflic");
            default:
                break;
        }
    }

    @Override
    public void deleteRulesFile() throws FileRulesException {
        final Response response = client.target(serviceUrl).path(RULESMANAGER_DELETE_URL)
            .request(MediaType.APPLICATION_OCTET_STREAM)
            .accept(MediaType.APPLICATION_JSON)
            .delete(Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case PRECONDITION_FAILED:
                LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new FileRulesException("File rules error");
            default:
                break;
        }
    }

    @Override
    public JsonNode getRuleByID(String id) throws FileRulesException, InvalidParseOperationException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);

        final Response response = client.target(serviceUrl).path(RULESMANAGER_URL + "/" + id)
            .request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(id, MediaType.APPLICATION_JSON), Response.class);

        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case NOT_FOUND:
                LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                throw new FileRulesException("File Rules not found");
            default:
                break;
        }
        return JsonHandler.getFromString(response.readEntity(String.class));
    }

    @Override
    public JsonNode getRule(JsonNode query)
        throws FileRulesException, InvalidParseOperationException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        final Response response = client.target(serviceUrl).path(RULESMANAGER_GET_DOCUMENT_URL)
            .request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(query, MediaType.APPLICATION_JSON), Response.class);
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                break;
            case NOT_FOUND:
                LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                throw new FileRulesException("Rule Not found ");
            default:
                break;
        }
        return JsonHandler.getFromString(response.readEntity(String.class));
    }
}
