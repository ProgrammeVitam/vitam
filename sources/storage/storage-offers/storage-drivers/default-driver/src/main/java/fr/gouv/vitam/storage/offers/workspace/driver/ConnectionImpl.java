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
package fr.gouv.vitam.storage.offers.workspace.driver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.PutObjectResult;
import fr.gouv.vitam.storage.driver.model.RemoveObjectRequest;
import fr.gouv.vitam.storage.driver.model.RemoveObjectResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;

/**
 * Workspace Connection Implementation
 */
// TODO: always close javax.ws.rs.core.Response (because can make problems)
public class ConnectionImpl implements Connection {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ConnectionImpl.class);

    // TODO: RESSOURCE_PATH came from driver initalisation
    private static final String RESOURCE_PATH = "/offer/v1";
    private static final String STATUS_PATH = "/status";
    private static final String OBJECTS_PATH = "/objects";
    private static final int CHUNK_SIZE = 1024 * 1024;

    private static final String INTERNAL_SERVER_ERROR =
        "Internal Server Error, could not connect to the distant offer service.";
    private static final String NOT_YET_IMPLEMENTED =
        "Not yet implemented";

    private static final String REQUEST_IS_A_MANDATORY_PARAMETER = "Request is a mandatory parameter";
    private static final String GUID_IS_A_MANDATORY_PARAMETER = "GUID is a mandatory parameter";
    private static final String TENANT_IS_A_MANDATORY_PARAMETER = "Tenant is a mandatory parameter";
    private static final String ALGORITHM_IS_A_MANDATORY_PARAMETER = "Algorithm is a mandatory parameter";
    private static final String STREAM_IS_A_MANDATORY_PARAMETER = "Stream is a mandatory parameter";
    private static final String TYPE_IS_A_MANDATORY_PARAMETER = "Type is a mandatory parameter";
    private static final String TYPE_IS_NOT_VALID = "Type is not valid";
    private static final String FOLDER_IS_A_MANDATORY_PARAMETER = "Folder is a mandatory parameter";
    private static final String FOLDER_IS_NOT_VALID = "Folder is not valid";
    private Client client;
    private String serviceUrl;
    private String driverName;

    /**
     * Constructor for the ConnectionImpl class
     * 
     * @param url the url
     * @param driverName the name of the driver
     */
    public ConnectionImpl(String url, String driverName) {
        this.serviceUrl = url + RESOURCE_PATH;
        this.driverName = driverName;
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        // TODO: multipart ?
        config.register(MultiPartFeature.class);
        client = ClientBuilder.newClient(config);
    }

    @Override
    public StorageCapacityResult getStorageCapacity(String tenantId) throws StorageDriverException {
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenantId);
        Response response = null;
        try {
            response = getClient().target(getServiceUrl()).path(OBJECTS_PATH).request(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_TENANT_ID, tenantId).method(HttpMethod.GET);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                StorageCapacityResult storageCapacityResult =
                    handleResponseStatus(response, StorageCapacityResult.class);
                return storageCapacityResult;
            }
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                response.getStatusInfo().getReasonPhrase());
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public GetObjectResult getObject(GetObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getFolder());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getFolder()));
        Response response = null;
        InputStream stream;
        try {
            response =
                getClient().target(getServiceUrl())
                    .path(OBJECTS_PATH + "/" + request.getFolder() + "/" + request.getGuid()).request()
                    .header(VitamHttpHeader.TENANT_ID.getName(), request.getTenantId())
                    .accept(MediaType.APPLICATION_OCTET_STREAM).method(HttpMethod.GET);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    // TODO : this is ugly but necessarily in order to close the response and avoid concurrent issues
                    // to be improved (https://jersey.java.net/documentation/latest/client.html#d0e5170) and
                    // remove the IOUtils.toByteArray after correction of concurrent problem
                    InputStream streamClosedAutomatically = response.readEntity(InputStream.class);
                    try {
                        stream = new ByteArrayInputStream(IOUtils.toByteArray(streamClosedAutomatically));
                    } catch (IOException e) {
                        LOGGER.error(e);
                        throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND,
                            e.getMessage());
                    }
                    GetObjectResult result = new GetObjectResult(request.getTenantId(), stream);
                    return result;
                case NOT_FOUND:
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, "Object " +
                        "not found");
                case PRECONDITION_FAILED:
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Precondition failed");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest request) throws StorageDriverException {
        Response response = null;
        try {
            ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
            ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
            ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
            ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestAlgorithm());
            ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
            ParametersChecker.checkParameter(TYPE_IS_NOT_VALID, DataCategory.valueOf(request.getType()));
            ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, request.getDataStream());

            InputStream stream = request.getDataStream();
            // init
            ObjectInit objectInit = new ObjectInit();
            objectInit.setDigestAlgorithm(DigestType.fromValue(request.getDigestAlgorithm()));
            objectInit.setType(DataCategory.valueOf(request.getType()));

            response =
                getClient().target(getServiceUrl()).path(OBJECTS_PATH + "/" + request.getGuid())
                    .request(MediaType.APPLICATION_JSON)
                    .headers(getDefaultHeaders(request.getTenantId(), StorageConstants.COMMAND_INIT))
                    .accept(MediaType.APPLICATION_JSON).method(HttpMethod.POST, Entity.json(objectInit));

            PutObjectResult finalResult =
                performPutRequests(request.getTenantId(), stream, handleResponseStatus(response, ObjectInit.class));
            return finalResult;
        } catch (IllegalArgumentException exc) {
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED, exc
                .getMessage());
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public RemoveObjectResult removeObject(RemoveObjectRequest request) throws StorageDriverException {
        throw new UnsupportedOperationException(NOT_YET_IMPLEMENTED);
    }

    @Override
    public Boolean objectExistsInOffer(GetObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        Response response = null;
        try {
            response =
                getClient().target(getServiceUrl()).path(OBJECTS_PATH + "/" + request.getGuid()).request()
                    .header(VitamHttpHeader.TENANT_ID.getName(), request.getTenantId())
                    .accept(MediaType.APPLICATION_OCTET_STREAM).method(HttpMethod.HEAD);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK: case NO_CONTENT:
                    return true;
                case NOT_FOUND:
                    return false;
                case BAD_REQUEST:
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Bad request");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    /**
     * Get The status of the service
     * 
     * @return response the response
     * @throws StorageDriverException if the status is not OK
     */
    public Response getStatus() throws StorageDriverException {
        Response response = null;
        try {
            response = getClient().target(serviceUrl).path(STATUS_PATH).request().get();
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response;
            } else {
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR);
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }

    }

    @Override
    public void close() throws StorageDriverException {
        if (this.getClient() != null) {
            this.getClient().close();
        }
    }

    /**
     * Get the service Url
     * 
     * @return the serviceUrl
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Get The client
     * 
     * @return the client
     */
    protected Client getClient() {
        return client;
    }

    /**
     * Common method to handle response status
     * 
     * @param response the response to be handled
     * @param responseType the type to map the response into
     * @param <R> the class type to be returned
     * @return the response mapped as a POJO
     * @throws StorageDriverException if any from the server
     */
    protected <R> R handleResponseStatus(Response response, Class<R> responseType)
        throws StorageDriverException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case CREATED:
            case OK:
                return response.readEntity(responseType);
            case INTERNAL_SERVER_ERROR:
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    status.getReasonPhrase());
            case NOT_FOUND:
                // TODO: make a *NotFoundException for this case
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, status
                    .getReasonPhrase());
            case SERVICE_UNAVAILABLE:
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, status
                    .getReasonPhrase());
            default:
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR);
        }
    }



    /**
     * Generate the default header map
     * 
     * @param tenantId the tenantId
     * @param command the command to be added
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String tenantId, String command) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        if (tenantId != null) {
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        }
        if (command != null) {
            headers.add(GlobalDataRest.X_COMMAND, command);
        }
        return headers;
    }


    /**
     * Method performing a PutRequests
     * 
     * @param tenantId the tenant Id
     * @param stream the stream to be chunked if necessary
     * @param result the result received from the server after the init
     * @return a PutObjectResult the final result received from the server
     * @throws StorageDriverException in case the server encounters an exception
     */
    private PutObjectResult performPutRequests(String tenantId, InputStream stream, ObjectInit result)
        throws StorageDriverException {
        PutObjectResult finalResult = null;
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(stream)) {
            ByteBuffer bb = ByteBuffer.allocate(CHUNK_SIZE);
            byte[] bytes;
            int read = readableByteChannel.read(bb);
            while (read >= 0) {
                bb.flip();
                if (read < CHUNK_SIZE) {
                    bytes = new byte[read];
                    bb.get(bytes, 0, read);
                    Entity<InputStream> entity =
                        Entity.entity(new ByteArrayInputStream(bytes), MediaType.APPLICATION_OCTET_STREAM);
                    Response response = null;
                    try {
                        // As it's the end of the file, END command is sent
                        response =
                            getClient().target(getServiceUrl()).path(OBJECTS_PATH + "/" + result.getId())
                                .request(MediaType.APPLICATION_OCTET_STREAM)
                                .headers(getDefaultHeaders(tenantId, StorageConstants.COMMAND_END))
                                .accept(MediaType.APPLICATION_JSON).method(HttpMethod.PUT, entity);
                        JsonNode json = handleResponseStatus(response, JsonNode.class);
                        finalResult = new PutObjectResult(result.getId(), json.get("digest").textValue(), tenantId);
                    } finally {
                        Optional.ofNullable(response).ifPresent(Response::close);
                    }
                } else {
                    bytes = bb.array();
                    Entity<InputStream> entity =
                        Entity.entity(new ByteArrayInputStream(bytes), MediaType.APPLICATION_OCTET_STREAM);
                    getClient().target(getServiceUrl()).path(OBJECTS_PATH + "/" + result.getId())
                        .request(MediaType.APPLICATION_OCTET_STREAM)
                        .headers(getDefaultHeaders(tenantId, StorageConstants.COMMAND_WRITE))
                        .accept(MediaType.APPLICATION_JSON).method(HttpMethod.PUT, entity);
                }
                bb.clear();
                read = readableByteChannel.read(bb);
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }
        return finalResult;
    }

}
